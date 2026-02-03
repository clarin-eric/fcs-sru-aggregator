#!/usr/bin/env python3

#
# FCS Statistic Log Analysis
#
# Collates fcsstats log entires about explain/search/result events into search usage information.
# Either pretty print or export as TSVs for futher analysis.
#
# Usage:
#   cat stats.log > ./logstatsanalysis.py -i - write
#   python3 logstatsanalysis.py -i prod/stats.log write -l prod
#   ./logstatsanalysis.py -i - write < <(zcat stats-2025*.log.gz)
#
#   ./logstatsanalysis.py -i - write -l alpha < <( { zcat alpha/stats-2025-*.log.gz ; cat alpha/stats.log ; } )
#   ./logstatsanalysis.py -i - write -l prod < <( { zcat prod/stats-2025-*.log.gz ; cat prod/stats.log ; } )
#
#   # show help
#   ./logstatsanalysis.py -h
#   ./logstatsanalysis.py print -h
#   ./logstatsanalysis.py write -h
#
#   # some more examples about pretty printing stats
#   python3 logstatsanalysis.py -i prod/stats.log -t lex print-searches
#   python3 logstatsanalysis.py -i - -t lex print print-timings --per-search --summary < <( { cat prod/stats*.log ; } )
#
#   cat searches*.tsv | cut -d$'\t' -f5 | sort | uniq -c | sort -rn
#


import argparse
import csv
import io
import re
import statistics
import sys
import typing
from datetime import timedelta, datetime
from itertools import groupby, chain
from functools import lru_cache
from pprint import pprint
from typing import Dict, List, Literal, Optional, Set, Tuple, TypedDict, TypeVar, Union
from urllib.parse import SplitResult, urlsplit


if hasattr(typing, "Required"):  # 3.11+
    NotRequired = typing.NotRequired
else:
    try:
        from typing_extensions import NotRequired
    except ImportError:

        import warnings

        warnings.warn("No NotRequired typing support")

        if sys.version_info[:2] >= (3, 9):  # 3.9-3.10

            @typing._SpecialForm
            def NotRequired(self, parameters):
                item = typing._type_check(
                    parameters, f"{self._name} accepts only a single type."
                )
                return typing._GenericAlias(self, (item,))

        else:

            NotRequired = Optional


# --------------------------------------------------------------------------

DEBUG = False

# --------------------------------------------------------------------------


class Colors:
    """ANSI color codes
    by rene-d 2018
    see: https://gist.github.com/rene-d/9e584a7dd2935d0f461904b9f2950007
    """

    BLACK = "\033[0;30m"
    RED = "\033[0;31m"
    GREEN = "\033[0;32m"
    BROWN = "\033[0;33m"
    BLUE = "\033[0;34m"
    PURPLE = "\033[0;35m"
    CYAN = "\033[0;36m"
    LIGHT_GRAY = "\033[0;37m"
    DARK_GRAY = "\033[1;30m"
    LIGHT_RED = "\033[1;31m"
    LIGHT_GREEN = "\033[1;32m"
    YELLOW = "\033[1;33m"
    LIGHT_BLUE = "\033[1;34m"
    LIGHT_PURPLE = "\033[1;35m"
    LIGHT_CYAN = "\033[1;36m"
    LIGHT_WHITE = "\033[1;37m"
    BOLD = "\033[1m"
    FAINT = "\033[2m"
    ITALIC = "\033[3m"
    UNDERLINE = "\033[4m"
    BLINK = "\033[5m"
    NEGATIVE = "\033[7m"
    CROSSED = "\033[9m"
    END = "\033[0m"

    # cancel SGR codes if we don't write to a terminal
    if not __import__("sys").stdout.isatty():
        for _ in dir():
            if isinstance(_, str) and _[0] != "_":
                locals()[_] = ""
    else:
        # set Windows console in VT mode
        if __import__("platform").system() == "Windows":
            kernel32 = __import__("ctypes").windll.kernel32
            kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
            del kernel32


def eprint(*values: object, **kwargs):
    print(*values, **kwargs, file=sys.stderr)


# --------------------------------------------------------------------------


class LogRecordExplain(TypedDict):
    endpoint: str
    resource: str
    languages: List[str]
    capabilities: List[str]


class LogRecordSearchInitial(TypedDict):
    queryType: str
    query: str
    language: str


class LogRecordSearchResource(TypedDict):
    endpoint: str
    resource: str
    sruversion: str
    batch_start: Union[Literal[1], int]
    batch_len: int
    queryType: Optional[str]
    query: Optional[str]


class LogRecordSearchInitialWithSearchId(LogRecordSearchInitial):
    searchid: str


class LogRecordSearchResourceWithSearchId(LogRecordSearchResource):
    searchid: str


# parse_search_resource()/LogRecordSearchResource
# parse_search_initial()/LogRecordSearchInitial
LogRecordSearch = Union[
    LogRecordSearchResourceWithSearchId, LogRecordSearchInitialWithSearchId
]


class LogRecordResult(TypedDict):
    searchid: str
    endpoint: str
    resource: str

    cancelled: NotRequired[bool]
    error: NotRequired[str]

    numberOfRecords: NotRequired[Union[Literal[-1], int]]
    nextRecord: NotRequired[int]

    timeExecMs: NotRequired[int]
    timeQueueMs: NotRequired[int]

    _is_multiline: NotRequired[bool]


LogRecordData = Union[LogRecordExplain, LogRecordSearch, LogRecordResult]


class LogRecordDateTime(TypedDict):
    date: str
    time: str
    dt: datetime


class LogRecordMaybeLineNr(TypedDict):
    _lno: NotRequired[int | None]


class LogRecordMeta(LogRecordDateTime, LogRecordMaybeLineNr):
    type: Literal["explain", "search", "result"]


class ExplainLogRecord(LogRecordDateTime, LogRecordMaybeLineNr, LogRecordExplain):
    type: Literal["explain"]


class InitialSearchRequestLogRecord(
    LogRecordDateTime, LogRecordMaybeLineNr, LogRecordSearchInitialWithSearchId
):
    type: Literal["search"]


class ResourceSearchRequestLogRecord(
    LogRecordDateTime, LogRecordMaybeLineNr, LogRecordSearchResource
):
    type: Literal["search"]


class ResourceResultLogRecord(LogRecordDateTime, LogRecordMaybeLineNr, LogRecordResult):
    type: Literal["result"]


LR = TypeVar(
    "LR",
    ExplainLogRecord,
    InitialSearchRequestLogRecord,
    ResourceSearchRequestLogRecord,
    ResourceResultLogRecord,
)

LogRecord = Union[
    ExplainLogRecord,
    InitialSearchRequestLogRecord,
    ResourceSearchRequestLogRecord,
    ResourceResultLogRecord,
]
NotExplainLogRecord = Union[
    InitialSearchRequestLogRecord,
    ResourceSearchRequestLogRecord,
    ResourceResultLogRecord,
]


# --------------------------------------------------------------------------


def chop_quoted_value(
    text: str,
    field: Optional[str],
    quotes: Optional[Union[str, Tuple[str, str]]] = "'",
    space_after: bool = True,
    from_end: bool = False,
) -> Tuple[str, str]:
    needle_start = ""
    if field:
        needle_start = f"{field}="
    needle_end = ""

    if quotes:
        if isinstance(quotes, str):
            quote_start = quote_end = quotes
        elif isinstance(quotes, (tuple, list)) and len(quotes) == 2:
            quote_start, quote_end = quotes

        needle_start = f"{needle_start}{quote_start}"
        needle_end = f"{quote_end}"
    if space_after:
        if from_end:
            needle_start = f" {needle_start}"
        else:
            needle_end = f"{needle_end} "

    if from_end:
        idx_end = text.rindex(needle_end)
        assert idx_end == len(text) - len(
            needle_end
        ), f"{idx_end=} {len(text)-len(needle_end)=} {text=}"

        if quotes or space_after:
            idx_start = text.rindex(needle_start, 0, idx_end)
        else:
            idx_start = 0

        value = text[idx_start + len(needle_start) : idx_end]
        text = text[:idx_start]

    else:
        idx_start = text.index(needle_start)
        assert idx_start == 0, f"{idx_start=} {text=}"

        if quotes or space_after:
            idx_end = text.index(needle_end, idx_start + len(needle_start))
        else:
            idx_end = len(text)

        value = text[idx_start + len(needle_start) : idx_end]
        text = text[idx_end + len(needle_end) :]

    return value, text


def parse_explain(msg: str) -> LogRecordExplain:
    endpoint, msg = chop_quoted_value(msg, "endpoint")
    resource, msg = chop_quoted_value(msg, "resource")
    languages, msg = chop_quoted_value(msg, "languages", quotes=("[", "]"))

    capabilities, msg = chop_quoted_value(
        msg, "capabilities", quotes=("[", "]"), space_after=False
    )

    info = {
        "endpoint": endpoint,
        "resource": resource,
        "languages": languages.split(", "),
        "capabilities": capabilities.split(", "),
    }

    return info


def parse_search_resource(
    msg: str,
) -> LogRecordSearchResource:
    endpoint, msg = chop_quoted_value(msg, "endpoint")
    resource, msg = chop_quoted_value(msg, "resource")
    sruversion, msg = chop_quoted_value(msg, "sruversion")

    if " " not in msg:
        # variant format, does not have queryType/query for resource as it is the same to the initial search logrecord
        batch, msg = chop_quoted_value(msg, "batch", quotes=None, space_after=False)
        batch_start, batch_len = batch.split("+")

        queryType = None
        query = None
    else:
        batch, msg = chop_quoted_value(msg, "batch", quotes=None, space_after=True)
        batch_start, batch_len = batch.split("+")
        queryType, msg = chop_quoted_value(msg, "queryType")

        needle = "query='"
        assert msg.startswith(needle) and msg.endswith("'")
        query = msg[len(needle) : -1]

    return {
        "endpoint": endpoint,
        "resource": resource,
        "sruversion": sruversion,
        "batch_start": int(batch_start),
        "batch_len": int(batch_len),
        "queryType": queryType,
        "query": query,
    }


def parse_search_initial(msg: str) -> LogRecordSearchInitial:
    queryType, msg = chop_quoted_value(msg, "queryType")
    language, msg = chop_quoted_value(msg, "language", from_end=True)

    needle = "query='"
    assert msg.startswith(needle) and msg.endswith("'")
    query = msg[len(needle) : -1]

    return {"queryType": queryType, "query": query, "language": language}


def parse_search(msg: str) -> LogRecordSearch:
    searchid, msg = chop_quoted_value(msg, None, quotes=("[", "]"))

    if msg.startswith("endpoint='"):
        return {"searchid": searchid, **parse_search_resource(msg)}

    if msg.startswith("queryType='"):
        return {"searchid": searchid, **parse_search_initial(msg)}

    return {"searchid": searchid}


def parse_result(msg: str) -> LogRecordResult:
    # extract optional timing data (from end)
    try:
        timeExecMs, msg = chop_quoted_value(
            msg, "timeExecMs", quotes=None, from_end=True
        )
        timeQueueMs, msg = chop_quoted_value(
            msg, "timeQueueMs", quotes=None, from_end=True
        )
    except ValueError:
        timeExecMs = timeQueueMs = None

    searchid, msg = chop_quoted_value(msg, None, quotes=("[", "]"))
    endpoint, msg = chop_quoted_value(msg, "endpoint")
    resource, msg = chop_quoted_value(msg, "resource")

    numberOfRecords = nextRecord = None

    cancelled = False
    needle = "cancelled=''"
    if msg.startswith(needle):
        cancelled = True
        msg = msg[len(needle) :]

    is_multiline_error = False
    needle = "error='"
    if msg.startswith(needle) and msg.endswith("'"):
        error = msg[len(needle) : -1]
    elif msg.startswith(needle):
        error = msg[len(needle) :]
        is_multiline_error = True
    else:
        error = None

        try:
            numberOfRecords, msg = chop_quoted_value(
                msg, "numberOfRecords", quotes=None
            )
            nextRecord, msg = chop_quoted_value(
                msg, "nextRecord", quotes=None, space_after=False
            )
        except ValueError:
            pass

    info: LogRecordResult = {
        "searchid": searchid,
        "endpoint": endpoint,
        "resource": resource,
    }

    if cancelled is True:
        info.update({"cancelled": True})

    if error is not None:
        info.update({"error": error})
        if is_multiline_error:
            info.update({"_is_multiline": True})

    if numberOfRecords is not None and nextRecord is not None:
        info.update(
            {
                "numberOfRecords": int(numberOfRecords),
                "nextRecord": int(nextRecord),
            }
        )

    if timeExecMs is not None and timeQueueMs is not None:
        info.update(
            {
                "timeExecMs": int(timeExecMs),
                "timeQueueMs": int(timeQueueMs),
            }
        )

    return info


@lru_cache(1)
def parse_result_rest(line: str):
    msg = line.rstrip()

    # extract optional timing data (from end)
    try:
        timeExecMs, msg = chop_quoted_value(
            msg, "timeExecMs", quotes=None, from_end=True
        )
        timeQueueMs, msg = chop_quoted_value(
            msg, "timeQueueMs", quotes=None, from_end=True
        )
    except ValueError:
        timeExecMs = timeQueueMs = None

    if msg.endswith("'"):
        error = msg[:-1]

    info = {}

    if error is not None:
        info.update({"error": error})

    if timeExecMs is not None and timeQueueMs is not None:
        info.update(
            {
                "timeExecMs": int(timeExecMs),
                "timeQueueMs": int(timeQueueMs),
            }
        )

    return info


@lru_cache(2)
def parse_logmsg(type: str, msg: str) -> LogRecordData:
    if type == "explain":
        return parse_explain(msg)

    if type == "search":
        return parse_search(msg)

    if type == "result":
        return parse_result(msg)

    assert False


@lru_cache(2)
def parse_logmeta(line: str) -> Tuple[LogRecordMeta, str]:
    line = line.rstrip()

    # extract log level
    loglevel, line = chop_quoted_value(line, None, quotes=None)
    assert (
        loglevel == "TRACE"
    ), f"Require logline to start with TRACE log level, found {loglevel=}"

    # strip variable padding
    line = line.lstrip()

    # extract timestamp
    logdatetime, line = chop_quoted_value(line, None, quotes=("[", "]"))
    logdatetime_parsed = datetime.strptime(logdatetime, "%Y-%m-%d %H:%M:%S,%f")
    # logdatetime_parsed = logdatetime_parsed.replace(microsecond=0)
    logdate, logtime = logdatetime.split(" ", 1)

    # extract logger
    logger, msg = line.split(": ", 1)
    assert logger.startswith(
        "fcsstats."
    ), f"Logger should start with 'fcsstats.', found {logger=}"

    type = logger.split(".", 1)[-1]

    data: LogRecordMeta = {
        "date": logdate,
        "time": logtime,
        "dt": logdatetime_parsed,
        # "date_p": logdatetime_parsed.date(),
        # "time_p": logdatetime_parsed.time(),
        "type": type,
    }

    return data, msg


def parse_logline(line: str) -> LogRecord:
    # process log metadata (loglevel, date, logger)
    data, msg = parse_logmeta(line)
    type = data["type"]

    # process message
    message_data = parse_logmsg(type, msg)
    data.update(message_data)

    return data


# --------------------------------------------------------------------------


def is_logline_start(line: str) -> bool:
    try:
        parse_logmeta(line)
        return True
    except:
        return False


def is_logline(line: str) -> bool:
    try:
        parse_logline(line)
        return True
    except:
        return False


# --------------------------------------------------------------------------


def get_TLD(parsed_url: SplitResult):
    """best effort TLD extraction"""
    # https://stackoverflow.com/q/1066933/9360161
    # https://stackoverflow.com/a/569219/9360161
    # https://github.com/clarin-eric/fcs-sru-aggregator-ui/blob/main/src/pages/Statistics/FCSStatistics.tsx

    hostname = parsed_url.hostname
    parts = hostname.split(".")

    # this should not be possible
    if len(parts) <= 1:
        return hostname

    lastTLDPart = parts[-1]

    # if only two parts, the last is the TLD
    if len(parts) == 2:
        return lastTLDPart

    # NOTE: heuristic to check if parts are too short...
    preLastTLDPart = parts[-2]
    if len(lastTLDPart) <= 3 and len(preLastTLDPart) == 2:
        return f"{preLastTLDPart}.{lastTLDPart}"

    return lastTLDPart


@lru_cache
def url2tld(url: str):
    parsed = urlsplit(url)
    tld = get_TLD(parsed)
    return tld


@lru_cache
def url2shortDomain(url: str, include_port: bool = True):
    # NOTE: assume `url` stats with scheme, required for `urlsplit()`
    parsed = urlsplit(url)
    tld = get_TLD(parsed)

    domain = parsed.hostname
    domainWithoutTLD = domain[: -len(tld) - 1]
    parts = domainWithoutTLD.split(".")
    mainDomainPart = parts[-1]
    shortDomain = f"{mainDomainPart}.{tld}"

    if include_port and parsed.port:
        return f"{shortDomain}:{parsed.port}"

    return shortDomain


# --------------------------------------------------------------------------

EndpointResourceTuple = Tuple[str, str]
DateTimeTuple = Tuple[str, str]
DateTimeTypeTuple = Tuple[DateTimeTuple, Literal["explain", "search/result"]]
GroupedLogRecords = Dict[
    DateTimeTypeTuple, Union[List[ExplainLogRecord], List[NotExplainLogRecord]]
]


class SearchInfo(TypedDict):
    date: str
    time: str
    dt: datetime
    searchid: str
    queryType: str
    query: str

    duration: Optional[float]  # timedelta.total_seconds()

    resources_available: int
    resources_searched: int
    resources_answered: int
    resources_searched_further: int

    requests_for_more_results: int
    result_hits_total: int
    resources_searched_languages: List[str]

    explain_records: List[ExplainLogRecord]
    search_record: List[InitialSearchRequestLogRecord]
    search_resource_records: List[ResourceSearchRequestLogRecord]
    result_records_initial: List[ResourceResultLogRecord]
    result_records_more: List[ResourceResultLogRecord]


class EndpointDurationInfo(TypedDict):
    start: datetime
    end: Optional[datetime]
    duration: Optional[timedelta]
    requests: int


class EndpointTimingsInfo(TypedDict):
    queue: List[int]
    exec: List[int]
    numRequests: List[int]
    duration: List[timedelta]


RegexFilterArg = Tuple[bool, re.Pattern]


# --------------------------------------------------------------------------


def filter_records(
    records: List[LogRecord],
    filter_endpoint_url: Optional[RegexFilterArg] = None,
    filter_resource_id: Optional[RegexFilterArg] = None,
) -> List[LogRecord]:
    # no filtering specified
    if filter_endpoint_url is None and filter_resource_id is None:
        return records

    records_filtered: List[LogRecord] = list()

    for record in records:
        # skip explain records?
        if record["type"] == "explain":
            # TODO: does this make sense?
            # would search in all/some check be valid after this?
            records_filtered.append(record)
            continue

        if filter_endpoint_url is not None:
            is_pos, pat = filter_endpoint_url
            endpointUrl = record.get("endpoint", None)
            if endpointUrl:
                match = pat.search(endpointUrl) is not None
                if is_pos != match:
                    continue

        if filter_resource_id is not None:
            is_pos, pat = filter_resource_id
            resourceId = record.get("resource", None)
            if resourceId:
                match = pat.search(resourceId) is not None
                if is_pos != match:
                    continue

        records_filtered.append(record)

    return records_filtered


def aggregate_records(
    records: List[LogRecord],
    threshold_explain_run: timedelta = timedelta(minutes=15),
) -> GroupedLogRecords:
    # aggregate search + result by searchid
    # order explain and insert first aggregated searchid by date

    map_searchid_records: Dict[str, List[NotExplainLogRecord]] = dict()
    list_explain_records: List[ExplainLogRecord] = list()

    for record in records:
        if record["type"] == "explain":
            list_explain_records.append(record)
            continue

        searchid = record["searchid"]
        try:
            map_searchid_records[searchid].append(record)
        except KeyError:
            map_searchid_records[searchid] = [record]

    # --------------------------------

    map_dt_explain_records: Dict[DateTimeTuple, List[ExplainLogRecord]] = dict()
    map_searchid_earliest: Dict[str, DateTimeTuple] = dict()

    # everything should usually already be sorted (except if multiple log files are merge in random order)
    def fn_get_dt(record: LogRecord) -> DateTimeTuple:
        return (record["date"], record["time"])

    # convenience method
    def fn_record_to_dt(record: ExplainLogRecord):
        return datetime.combine(
            date=datetime.strptime(record["date"], "%Y-%m-%d").date(),
            time=datetime.strptime(record["time"], "%H:%M:%S,%f").time(),
        )

    list_explain_records.sort(key=fn_get_dt)

    # we want to try to chunk explain records by timestamp (if large gap, e.g., more than 15min?, then it is next scan)
    # NOTE: to account for parallel explain and search processes
    if list_explain_records:
        earliest_explain_record = list_explain_records[0]
        cur_dt_tuple = fn_get_dt(earliest_explain_record)
        cur_dt = fn_record_to_dt(list_explain_records[0])
        map_dt_explain_records[cur_dt_tuple] = [earliest_explain_record]

        for record in list_explain_records[1:]:
            record_dt = fn_record_to_dt(record)
            if record_dt - cur_dt > threshold_explain_run:
                cur_dt_tuple = fn_get_dt(record)
                map_dt_explain_records[cur_dt_tuple] = [record]
            else:
                map_dt_explain_records[cur_dt_tuple].append(record)
            cur_dt = record_dt

    # find earliest time for search/result records
    for searchid, records in map_searchid_records.items():
        # TODO: sort by initial search request?
        # NOTE: initial search request might not exist if crossing days
        records.sort(key=fn_get_dt)
        first_record = records[0]
        map_searchid_earliest[searchid] = (first_record["date"], first_record["time"])

    # --------------------------------
    # try to find "insertion point" for search/result date into explain records

    list_dt_with_type: List[DateTimeTypeTuple] = list()

    for dt in map_dt_explain_records.keys():
        list_dt_with_type.append((dt, "explain"))
    for dt in map_searchid_earliest.values():
        list_dt_with_type.append((dt, "search/result"))

    list_dt_with_type.sort()

    # --------------------------------
    # filter out any un-necessary explains (i.e., explain without any search/result before the next explain)

    list_dt_with_type_filtered: List[DateTimeTypeTuple] = list()

    if list_dt_with_type:
        # only add previous one if next one is a search/result
        # and add any search/result
        cur_dt, cur_type = list_dt_with_type[0]
        for dt, type in list_dt_with_type[1:]:
            if cur_type == "explain" and type == "search/result":
                list_dt_with_type_filtered.append((cur_dt, cur_type))
            elif cur_type == "search/result":
                list_dt_with_type_filtered.append((cur_dt, cur_type))
            cur_dt, cur_type = dt, type

        # add last one (only if search/result)
        if cur_type == "search/result":
            list_dt_with_type_filtered.append((cur_dt, cur_type))

    list_dt_with_type = list_dt_with_type_filtered

    # --------------------------------
    # build map with records

    map_dt_with_type_records: Dict[
        DateTimeTypeTuple, Union[List[ExplainLogRecord], List[NotExplainLogRecord]]
    ] = dict()

    map_dt_searchid: Dict[DateTimeTuple, str] = {
        dt: searchid for searchid, dt in map_searchid_earliest.items()
    }

    for dt, type in list_dt_with_type:
        if type == "explain":
            map_dt_with_type_records[(dt, type)] = map_dt_explain_records[dt]
        elif type == "search/result":
            searchid = map_dt_searchid[dt]
            map_dt_with_type_records[(dt, type)] = map_searchid_records[searchid]

    return map_dt_with_type_records


def build_search_infos(
    grouped_records: GroupedLogRecords, logs_were_filtered: bool = False
) -> Optional[List[SearchInfo]]:
    if not grouped_records:
        return None

    def count_dedup_by_resource(records: List[LogRecord]) -> int:
        uniquer: Set[EndpointResourceTuple] = {
            (r["endpoint"], r["resource"]) for r in records
        }
        return len(uniquer)

    def get_initial_result_records_by_resource(
        records: List[LR],
    ) -> Tuple[List[LR], List[LR]]:
        # group by endpoint/resource
        map_resource_records: Dict[EndpointResourceTuple, List[LR]] = dict()
        for record in records:
            try:
                map_resource_records[(record["endpoint"], record["resource"])].append(
                    record
                )
            except KeyError:
                map_resource_records[(record["endpoint"], record["resource"])] = [
                    record
                ]

        # get first result record each and rest
        first_records: List[LR] = list()
        more_records: List[LR] = list()
        for records in map_resource_records.values():
            if len(records) == 1:
                first_records.append(records[0])
            else:
                records.sort(
                    key=lambda record: (
                        record["date"],
                        record["time"],
                        # records["nextRecord"],
                    )
                )
                # NOTE: timestamps should be far enough apart that sorting on date+time is enough
                # records_with_next = [r for r in records if r["nextRecord"] != -1]
                first_records.append(records[0])
                more_records.extend(records[1:])

        return (first_records, more_records)

    list_search_info: List[SearchInfo] = list()
    last_explain: Optional[DateTimeTypeTuple] = None

    for (dt, type), records in grouped_records.items():
        if type == "explain":
            last_explain = (dt, type)
            continue

        if type == "search/result":
            # initial search request (search parameters)

            search_request: Optional[InitialSearchRequestLogRecord] = next(
                (r for r in records if r["type"] == "search" and "endpoint" not in r),
                None,
            )
            if search_request is None:
                if DEBUG:
                    search_request = next(
                        (r for r in records if r["type"] == "search"),
                        {"searchid": None, "_lno": None},
                    )
                    searchid = search_request["searchid"]
                    lno = search_request["_lno"]
                    eprint(
                        f"Did not find initial search request: {dt=}, {type=}, {searchid=}, {lno=}"
                    )

                continue

            # TODO: do we need to assert that there is only ever one?
            assert (
                search_request is not None
            ), f"must have initial search request! {dt=} {type=}"

            # ------------------------
            # explain --> available resources

            # compute the list of available resources based on query type
            num_avail_resources: int = 0
            explain_records_for_capability: List[ExplainLogRecord] = list()

            if last_explain:
                explain_records: List[ExplainLogRecord] = grouped_records[last_explain]
                wanted_capability = "BASIC_SEARCH"
                if search_request["queryType"] == "lex":
                    wanted_capability = "LEX_SEARCH"
                elif search_request["queryType"] == "fcs":
                    wanted_capability = "ADVANCED_SEARCH"
                explain_records_for_capability = [
                    r for r in explain_records if wanted_capability in r["capabilities"]
                ]

                num_avail_resources = len(explain_records_for_capability)

            # ------------------------
            # search requests (for resources)

            search_resources_records = [
                r for r in records if r["type"] == "search" and "endpoint" in r
            ]
            if not logs_were_filtered and len(search_resources_records) == 0:
                # do not fail completely, due to filtering this might be reduced
                if DEBUG:
                    eprint(
                        f"[{search_request['searchid']}] must have search request records"
                    )
                continue

            search_resources_records_initial = [
                r for r in search_resources_records if r["batch_start"] == 1
            ]
            search_resources_records_more = [
                r for r in search_resources_records if r["batch_start"] > 1
            ]

            num_used_resources: int = len(search_resources_records_initial)
            num_requests_for_more_results: int = len(search_resources_records_more)

            set_resource_search_records: Set[EndpointResourceTuple] = {
                (r["endpoint"], r["resource"]) for r in search_resources_records
            }

            # set of languages of resources the search was performed in
            resources_searched_languages: Set[str] = {
                language
                for resource in explain_records_for_capability
                if (resource["endpoint"], resource["resource"])
                in set_resource_search_records
                for language in resource["languages"]
            }

            # ------------------------
            # search results

            result_records = [r for r in records if r["type"] == "result"]
            # NOTE: might be empty, on error maybe?
            # assert len(result_records) > 0, "should have search result records"

            result_records_firsts, result_records_more = (
                get_initial_result_records_by_resource(result_records)
            )
            result_counts: List[int] = [
                r["numberOfRecords"]
                for r in result_records_firsts
                if "numberOfRecords" in r and r["numberOfRecords"] != -1
            ]

            num_result_count_total = sum(result_counts)

            # ------------------------
            # search duration

            search_start_dts = [
                r["dt"] for r in search_resources_records_initial if r["dt"]
            ]
            search_start_dt = min(search_start_dts) if search_start_dts else None
            search_end_dts = [r["dt"] for r in result_records_firsts if r["dt"]]
            search_end_dt = max(search_end_dts) if search_end_dts else None

            duration = (
                (search_end_dt - search_start_dt).total_seconds()
                if search_start_dt and search_end_dt
                else None
            )

            # ------------------------

            search_info: SearchInfo = {
                # copy all relevant information about search
                **{
                    key: value
                    for key, value in search_request.items()
                    if key in ("date", "time", "dt", "searchid", "queryType", "query")
                },
                # search duration
                "duration": duration,
                # computed information about resources
                "resources_available": num_avail_resources,
                "resources_searched": num_used_resources,
                "resources_answered": count_dedup_by_resource(result_records),
                "resources_searched_further": count_dedup_by_resource(
                    search_resources_records_more
                ),
                # total "clicks" for more results
                "requests_for_more_results": num_requests_for_more_results,
                "resources_searched_languages": sorted(resources_searched_languages),
                "result_hits_total": num_result_count_total,
                # "original" log records
                "explain_records": explain_records_for_capability,
                "search_record": search_request,
                # search request records for initial and further searches
                "search_resource_records": search_resources_records,
                # result records from initial response
                "result_records_initial": result_records_firsts,
                # result records for further responses if requested by user
                "result_records_more": result_records_more,
            }

            # DEBUG: drop verbose stuff
            # del (
            #     search_info["resources_searched_languages"],
            #     search_info["search_record"],
            #     search_info["search_resource_records"],
            #     search_info["result_records_initial"],
            #     search_info["result_records_more"]
            # )

            list_search_info.append(search_info)

    return list_search_info


def filter_search_infos(
    search_infos: Optional[List[SearchInfo]],
    filter_queryType: Optional[Literal["lex", "cql", "fcs"]] = None,
    filter_hasResults: Optional[bool] = None,
    filter_allResources: Optional[bool] = None,
    filter_requestedMore: Optional[bool] = None,
) -> Optional[List[SearchInfo]]:
    if not search_infos:
        return

    # no filtering specified
    if (
        filter_queryType is None
        and filter_hasResults is None
        and filter_allResources is None
        and filter_requestedMore is None
    ):
        return search_infos

    # filter / output control
    search_infos_filtered: List[SearchInfo] = list()
    for search_info in search_infos:
        # search query type: <type>/skip
        if filter_queryType is not None:
            if search_info["queryType"] != filter_queryType:
                continue

        # search on all resources: yes/no/skip
        if filter_allResources is not None:
            has_used_all_resources = (
                search_info["resources_available"] == search_info["resources_searched"]
            )
            if has_used_all_resources != filter_allResources:
                continue

        # search has results: yes/no/skip
        if filter_hasResults is not None:
            has_results = search_info["result_hits_total"] > 0
            if has_results != filter_hasResults:
                continue

        # search (has records) and user requested more records from some resources
        if filter_requestedMore is not None:
            searched_further = search_info["resources_searched_further"] > 0
            if searched_further != filter_requestedMore:
                continue

        search_infos_filtered.append(search_info)

    return search_infos_filtered


def transform_search_infos(
    search_infos: Optional[List[SearchInfo]],
    transform_map_endpoint_urls_to_domain: bool = False,
    transform_map_endpoint_urls_to_domain__include_port: bool = True,
    transform_map_endpoint_urls_to_tld: bool = False,
) -> Optional[List[SearchInfo]]:
    if not search_infos:
        return

    if not (
        transform_map_endpoint_urls_to_tld or transform_map_endpoint_urls_to_domain
    ):
        return search_infos

    search_infos_transformed: List[SearchInfo] = list()

    for search_info in search_infos:
        search_info_transformed: SearchInfo = dict(search_info)  # NOTE: shallow copy!

        # transform endpoint URL
        if transform_map_endpoint_urls_to_tld or transform_map_endpoint_urls_to_domain:
            for key in (
                "explain_records",
                "search_resource_records",
                "result_records_initial",
                "result_records_more",
            ):
                records = search_info_transformed[key]
                records_transformed: List[LogRecord] = list()

                for record in records:
                    record_transformed: LogRecord = dict(record)
                    endpointUrl = record["endpoint"]

                    if transform_map_endpoint_urls_to_tld:
                        endpointUrl = url2tld(endpointUrl)
                    elif transform_map_endpoint_urls_to_domain:
                        endpointUrl = url2shortDomain(
                            endpointUrl,
                            include_port=transform_map_endpoint_urls_to_domain__include_port,
                        )

                    record_transformed["endpoint"] = endpointUrl
                    records_transformed.append(record_transformed)

                search_info_transformed[key] = records_transformed

        search_infos_transformed.append(search_info_transformed)

    return search_infos_transformed


def _mapSearchInfo2EndpointTimings(search_info: SearchInfo):
    results = search_info["result_records_initial"] + search_info["result_records_more"]

    endpointTimings = [
        (r["endpoint"], r["timeQueueMs"], r["timeExecMs"])
        for r in results
        if "timeQueueMs" in r and "timeExecMs" in r
    ]
    endpointTimings = sorted(endpointTimings, key=lambda r: (r[0],))

    # map endpoint to queue/exec timings lists
    endpoint2timings: Dict[str, Dict[Literal["queue", "exec"], List[int]]] = {
        endpoint: {
            t: list(vals)
            for t, vals in zip(("queue", "exec"), list(zip(*list(records)))[1:])
        }
        for endpoint, records in groupby(endpointTimings, key=lambda r: r[0])
    }

    return endpoint2timings


def _mapSearchInfo2EndpointDurations(search_info: SearchInfo):
    # search: LogRecord = search_info["search_record"]
    # search_start: datetime = search["dt"]

    # NOTE: ignore user requests for more results (only interested in initial search request for now)
    # further search result requests can happen at "any" time so an overall duration is difficult (to compute and interpret)

    searches = search_info["search_resource_records"]
    searches_initial = [r for r in searches if r["batch_start"] == 1]
    # searches_more = [r for r in searches if r["batch_start"] > 1]

    results_initial = search_info["result_records_initial"]
    # results_more = search_info["result_records_more"]

    endpointTimeStart = [(r["endpoint"], r["dt"]) for r in searches_initial]
    endpointTimeStart = sorted(endpointTimeStart, key=lambda r: (r[0],))
    endpoint2Requests: Dict[str, int] = {
        endpoint: len(list(records))
        for endpoint, records in groupby(endpointTimeStart, key=lambda r: r[0])
    }
    endpoint2timeStart: Dict[str, List[datetime]] = {
        endpoint: min({r[1] for r in records})
        for endpoint, records in groupby(endpointTimeStart, key=lambda r: r[0])
    }

    endpointTimeEnd = [(r["endpoint"], r["dt"]) for r in results_initial]
    endpointTimeEnd = sorted(endpointTimeEnd, key=lambda r: (r[0],))
    endpoint2timeEnd: Dict[str, List[datetime]] = {
        endpoint: max({r[1] for r in records})
        for endpoint, records in groupby(endpointTimeEnd, key=lambda r: r[0])
    }

    endpointDurations: Dict[str, EndpointDurationInfo] = {
        endpoint: {
            "start": endpoint2timeStart[endpoint],
            "end": endpoint2timeEnd[endpoint] if endpoint in endpoint2timeEnd else None,
            "duration": (
                (endpoint2timeEnd[endpoint] - endpoint2timeStart[endpoint])
                if endpoint in endpoint2timeEnd
                else None
            ),
            "requests": endpoint2Requests[endpoint],
        }
        for endpoint in endpoint2timeStart.keys()
    }

    # detect missing endpoints
    endpoints = sorted(endpoint2timeStart.keys() | endpoint2timeEnd.keys())
    endpoints = [
        endpoint
        for endpoint in endpoints
        if endpoint not in endpoint2timeStart or endpoint not in endpoint2timeEnd
    ]
    if DEBUG and endpoints:
        print()
        print(f"####    {search_info['searchid']}")

        p = " " * 26

        # all log records
        m1 = min({r[1] for r in endpointTimeStart})
        m2_i = {r[1] for r in endpointTimeEnd}
        m2 = max(m2_i) if m2_i else p
        print(f"####[1] {m1} -- {m2} : {(m2-m1).total_seconds() if m2_i else p} ####")

        # processed log records
        m1 = min({r["start"] for r in endpointDurations.values()})
        m2_i = {r["end"] for r in endpointDurations.values() if r["end"]}
        m2 = max(m2_i) if m2_i else p
        print(f"####[2] {m1} -- {m2} : {(m2-m1).total_seconds() if m2_i else p} ####")

        # where start/end is missing for endpoints (due to error or aggregator restart or other reasons)
        for endpoint in endpoints:
            m3 = endpoint2timeStart[endpoint] if endpoint in endpoint2timeStart else p
            m4 = endpoint2timeEnd[endpoint] if endpoint in endpoint2timeEnd else p
            print(f"####[3] {m3} -- {m4} : {endpoint} ####")

        print()

    return endpointDurations


# --------------------------------------------------------------------------


def print_search_info_report(search_infos: Optional[List[SearchInfo]]):
    if not search_infos:
        return

    for i, search_info in enumerate(search_infos):
        if i > 0:
            print("-" * 70)

        search_date = search_info["date"]
        search_time = search_info["time"]

        print(
            f"Search ID: {Colors.GREEN}{search_info['searchid']}{Colors.END} -- {Colors.ITALIC}{search_date} {search_time}{Colors.END}"
        )
        print(
            f"  on {Colors.GREEN}{search_info['date']}{Colors.END}"
            f" at {Colors.GREEN}{search_info['time']}{Colors.END}"
        )
        print(
            f"  queryType = {Colors.LIGHT_CYAN}{search_info['queryType']}{Colors.END}"
            f" query = >>{Colors.LIGHT_BLUE}{search_info['query']}{Colors.END}<<"
        )

        print(
            f"  search through {Colors.RED}{search_info['resources_searched']:3d}{Colors.END}"
            f" / {Colors.RED}{search_info['resources_available']:3d}{Colors.END} resources"
        )
        if search_info["resources_searched_further"]:
            print(
                f"    requested more results from {Colors.LIGHT_RED}{search_info['resources_searched_further']}{Colors.END} resources"
            )

        if search_info["result_hits_total"]:
            result_counts = [
                r["numberOfRecords"]
                for r in search_info["result_records_initial"]
                if "numberOfRecords" in r and r["numberOfRecords"] != -1
            ]

            print(
                f"  --> got {Colors.LIGHT_GREEN}{search_info['result_hits_total']}{Colors.END} total results"
                f" in {Colors.LIGHT_RED}{len(result_counts)}{Colors.END} resources"
            )
            if result_counts and len(result_counts) > 1:
                print(
                    f"          = {Colors.DARK_GRAY}{(Colors.END+'+'+Colors.DARK_GRAY).join(map(str, result_counts))}{Colors.END}"
                )


def print_timing_report(
    search_infos: Optional[List[SearchInfo]],
    per_search: bool = True,
    per_endpoint: bool = True,
    summary: bool = True,
):
    if not search_infos:
        return

    def _formatTimings(timings: List[int]):
        numTimings = len(timings)
        minTime = min(timings)
        maxTime = max(timings)
        meanTime = statistics.mean(timings)
        stddevTime = statistics.stdev(timings, meanTime) if numTimings > 1 else 0

        parts = list()
        if maxTime == 0 or minTime == maxTime:
            parts.append(f"{Colors.DARK_GRAY}{minTime}{Colors.END} ms")
        else:
            parts.append(
                f"{Colors.DARK_GRAY}{minTime}{Colors.END}-{Colors.DARK_GRAY}{maxTime}{Colors.END} ms (min/max)"
            )
            if stddevTime != 0:
                parts.append(
                    f", mean: {Colors.RED}{meanTime:.2f}{Colors.END} ±{Colors.RED}{stddevTime:.2f}{Colors.END} ms"
                )
            else:
                parts.append(f", mean: {Colors.RED}{meanTime:.2f}{Colors.END} ms")
        parts.append(
            f" for {Colors.DARK_GRAY}{numTimings}{Colors.END} request{'s' if numTimings != 1 else ''} (resource{'s' if numTimings != 1 else ''})"
        )

        return "".join(parts)

    def _formatNumRequests(numRequests: List[int], addSearches: bool = True):
        numSearches = len(numRequests)
        minReq = min(numRequests)
        maxReq = max(numRequests)
        meanReq = statistics.mean(numRequests)
        stddevReq = statistics.stdev(numRequests, meanReq) if numSearches > 1 else 0

        parts = list()
        if maxReq == 0 or minReq == maxReq:
            parts.append(f"{Colors.DARK_GRAY}{minReq}{Colors.END}")
        else:
            parts.append(
                f"{Colors.DARK_GRAY}{minReq}{Colors.END}-{Colors.DARK_GRAY}{maxReq}{Colors.END}"
            )
            if stddevReq != 0:
                parts.append(
                    f", mean: {Colors.RED}{meanReq:.2f}{Colors.END} ±{Colors.RED}{stddevReq:.2f}{Colors.END}"
                )
            else:
                parts.append(f", mean: {Colors.RED}{meanReq:.2f}{Colors.END}")
            parts.append(f", total: {Colors.RED}{sum(numRequests)}{Colors.END}")

        if addSearches:
            parts.append(
                f" in {Colors.DARK_GRAY}{numSearches}{Colors.END} search{'es' if numSearches != 1 else ''}"
            )

        return "".join(parts)

    def _formatDuration(durationInfo: Dict[str, datetime | timedelta | int]):
        numRequest = durationInfo["requests"]

        parts = list()
        if durationInfo["duration"] is not None:
            parts.append(
                f"{Colors.RED}{durationInfo['duration'].total_seconds()}{Colors.END} sec"
            )
        else:
            parts.append(f"{Colors.RED}{Colors.BLINK}???{Colors.END} sec")
        parts.append(
            f" for {Colors.DARK_GRAY}{numRequest}{Colors.END} request{'s' if numRequest != 1 else ''} (resource{'s' if numRequest != 1 else ''})"
        )

        return "".join(parts)

    def _formatDurationsMean(durations: List[timedelta], addSearches: bool = True):
        durations = [d.total_seconds() for d in durations]
        numDurations = len(durations)
        minDurations = min(durations)
        maxDurations = max(durations)
        meanDurations = statistics.mean(durations)
        stddevDurations = (
            statistics.stdev(durations, meanDurations) if numDurations > 1 else 0
        )

        parts = list()

        if maxDurations == 0 or minDurations == maxDurations:
            parts.append(f"{Colors.DARK_GRAY}{minDurations}{Colors.END} mssec")
        else:
            parts.append(
                f"{Colors.DARK_GRAY}{minDurations:.2f}{Colors.END}-{Colors.DARK_GRAY}{maxDurations:.2f}{Colors.END} sec (min/max)"
            )

            if stddevDurations != 0:
                parts.append(
                    f", mean: {Colors.RED}{meanDurations:.2f}{Colors.END} ±{Colors.RED}{stddevDurations:.2f}{Colors.END} sec"
                )
            else:
                parts.append(f", mean: {Colors.RED}{meanDurations:.2f}{Colors.END} sec")

        if addSearches:
            parts.append(
                f" for {Colors.DARK_GRAY}{numDurations}{Colors.END} search{'es' if numDurations != 1 else ''}"
            )

        return "".join(parts)

    # mapping of endpoint to timings lists
    combinedEndpoint2timings: Dict[str, EndpointTimingsInfo] = dict()

    for i, search_info in enumerate(search_infos):
        if per_search and i > 0:
            print("-" * 70)

        search_date = search_info["date"]
        search_time = search_info["time"]

        if per_search:
            print(
                f"Search ID: {Colors.GREEN}{search_info['searchid']}{Colors.END} -- {Colors.ITALIC}{search_date} {search_time}{Colors.END}"
            )
            print(
                f"  queryType = {Colors.LIGHT_CYAN}{search_info['queryType']}{Colors.END}"
                f" query = >>{Colors.LIGHT_BLUE}{search_info['query']}{Colors.END}<<"
            )

        endpoint2timings = _mapSearchInfo2EndpointTimings(search_info)
        endpoint2duration = _mapSearchInfo2EndpointDurations(search_info)
        endpoints = sorted(endpoint2timings.keys() | endpoint2duration.keys())

        for endpoint in endpoints:
            durationInfo = queueTimings = execTimings = None

            if endpoint not in combinedEndpoint2timings:
                combinedEndpoint2timings[endpoint] = {
                    "queue": [],
                    "exec": [],
                    "numRequests": [],
                    "duration": [],
                }

            if endpoint in endpoint2timings:
                timings = endpoint2timings[endpoint]
                queueTimings = timings["queue"]
                execTimings = timings["exec"]
                numRequests = len(execTimings)

                combinedEndpoint2timings[endpoint]["queue"].extend(queueTimings)
                combinedEndpoint2timings[endpoint]["exec"].extend(execTimings)
                combinedEndpoint2timings[endpoint]["numRequests"].append(numRequests)

            if endpoint in endpoint2duration:
                durationInfo = endpoint2duration[endpoint]

                combinedEndpoint2timings[endpoint]["duration"].append(
                    durationInfo["duration"]
                )

            if per_search and per_endpoint:
                print(f"  - {Colors.BROWN}{endpoint}{Colors.END}")
                if durationInfo:
                    print(f"      Duration: {_formatDuration(durationInfo)}")
                if queueTimings and execTimings:
                    print(f"      Queue:     {_formatTimings(queueTimings)}")
                    print(f"      Execution: {_formatTimings(execTimings)}")

        if per_search and (summary or not per_endpoint):
            print("  Overall for search:")

            overallStart_i = [d["start"] for d in endpoint2duration.values()]
            overallStart = min(overallStart_i) if overallStart_i else None
            overallEnd_i = [
                d["end"] for d in endpoint2duration.values() if d["end"] is not None
            ]
            overallEnd = max(overallEnd_i) if overallEnd_i else None
            numRequests = [d["requests"] for d in endpoint2duration.values()]

            durationInfo: EndpointDurationInfo = {
                "start": overallStart,
                "end": overallEnd,
                "duration": (
                    (overallEnd - overallStart)
                    if overallStart_i and overallEnd_i
                    else None
                ),
                "requests": sum(numRequests) if numRequests else -1,
            }

            print(f"    Duration:  {_formatDuration(durationInfo)}")

            if len(endpoint2timings) > 1:
                queueTimings = list(
                    chain(*[t["queue"] for t in endpoint2timings.values()])
                )
                execTimings = list(
                    chain(*[t["exec"] for t in endpoint2timings.values()])
                )

                print(f"    Queue:     {_formatTimings(queueTimings)}")
                print(f"    Execution: {_formatTimings(execTimings)}")

    num_searches = len(search_infos)
    if summary and (num_searches > 1 or len(combinedEndpoint2timings) > 1):
        if per_search:
            print("-" * 70)

        queueTimings = list(
            chain(*[t["queue"] for t in combinedEndpoint2timings.values()])
        )
        execTimings = list(
            chain(*[t["exec"] for t in combinedEndpoint2timings.values()])
        )
        numRequests = list(
            chain(*[t["numRequests"] for t in combinedEndpoint2timings.values()])
        )

        durations = [
            d
            for d in chain(*[t["duration"] for t in combinedEndpoint2timings.values()])
            if d is not None
        ]

        print(
            f"{Colors.BOLD}Overall for all {Colors.GREEN}{num_searches}{Colors.END}{Colors.BOLD} searches:{Colors.END}"
        )

        if durations:
            print(f"  Duration:  {_formatDurationsMean(durations, False)}")
        if queueTimings:
            print(f"  Queue:     {_formatTimings(queueTimings)}")
        if execTimings:
            print(f"  Execution: {_formatTimings(execTimings)}")
        if numRequests:
            print(
                f"  Requests (resources) per search: {_formatNumRequests(numRequests, False)}"
            )

        if per_endpoint:
            for endpoint, timings in combinedEndpoint2timings.items():
                queueTimings = timings["queue"]
                execTimings = timings["exec"]
                numRequests = timings["numRequests"]

                durations = timings["duration"]
                durations = [d for d in durations if d is not None]

                print(f"  - {Colors.BROWN}{endpoint}{Colors.END}")
                if durations:
                    print(f"      Duration:  {_formatDurationsMean(durations)}")
                if queueTimings:
                    print(f"      Queue:     {_formatTimings(queueTimings)}")
                if execTimings:
                    print(f"      Execution: {_formatTimings(execTimings)}")
                if numRequests:
                    print(
                        f"      Requests (resources) per search: {_formatNumRequests(numRequests)}"
                    )


# --------------------------------------------------------------------------

# 'excel', 'excel-tab', 'unix'
csv.register_dialect(
    "tsv",
    delimiter="\t",
    quoting=csv.QUOTE_NONE,
    quotechar=None,
    escapechar=None,
    lineterminator="\n",
)

CSV_DIALECTS = Literal["tsv", "excel", "excel-tab", "unix"]


def write_searches_TSV(
    search_infos: Optional[List[SearchInfo]],
    filename: Union[str, int],
    csv_dialect: CSV_DIALECTS = "tsv",
):
    if not search_infos:
        return

    def escape_query(query: str) -> str:
        return query.replace("\t", " ").replace("\r", "\n").replace("\n", " ")

    with open(filename, "w", newline="") as fp:
        writer = csv.writer(fp, dialect=csv_dialect)

        fields = [
            # UUID-4 search identifier
            "searchid",
            # YYYY-mm-dd
            "date",
            # HH:MM:SS,fff (fff being milliseconds)
            "time",
            # "lex"|"cql"|"fcs"
            "queryType",
            # full query string
            "query",
            # what resources were available for the query type
            "resources_available",
            # what resource subselection was the search performed on (see "resources_available")
            "resources_searched",
            # for how many resources more results were being requested
            "resources_searched_further",
            # how many resources responded to the search (i.e., did not fail with error or we never got an answer; see "resources_searched")
            "resources_answered",
            # total sum of available hits (not all all loaded, by default only 10 per resource; see "resources_answered")
            "result_hits_total",
            # ISO 639-3 language codes of the resources the search was performed on (separeated by "|" (pipe) character; see "resources_searched")
            "resources_searched_languages",
            # duration for full search in seconds
            "duration_sec",
        ]
        writer.writerow(fields)

        for search_info in search_infos:
            writer.writerow(
                [
                    search_info["searchid"],
                    search_info["date"],
                    search_info["time"],
                    search_info["queryType"],
                    escape_query(search_info["query"]),
                    search_info["resources_available"],
                    search_info["resources_searched"],
                    search_info["resources_searched_further"],
                    search_info["resources_answered"],
                    search_info["result_hits_total"],
                    "|".join(search_info["resources_searched_languages"]),
                    search_info["duration"],
                ]
            )


def write_search_endpoint_timings_TSV(
    search_infos: Optional[List[SearchInfo]],
    filename: Union[str, int],
    csv_dialect: CSV_DIALECTS = "tsv",
    round_digits: int = 4,
):
    if not search_infos:
        return

    def optional_round(value: float):
        if round_digits is None:
            return value
        return round(value, round_digits)

    with open(filename, "w", newline="") as fp:
        writer = csv.writer(fp, dialect=csv_dialect)

        fields = [
            # UUID-4 search identifier
            "searchid",
            # endpoint URL
            "endpoint",
            # number of resources requested in search
            "num_resource_requests",
            # duration for full search in seconds
            "duration_sec",
            # queue: min/max/mean/stddev time - time spent waiting in request queue
            "queue_min_time",
            "queue_max_time",
            "queue_mean_time",
            "queue_stddev_time",
            # how many datapoints for queue
            "queue_num_timings",
            # exec: min/max/mean/stddev time - time spent executing search request (for single resource/endpoint)
            "exec_min_time",
            "exec_max_time",
            "exec_mean_time",
            "exec_stddev_time",
            # how many datapoints for exec
            "exec_num_timings",
        ]
        writer.writerow(fields)

        for search_info in search_infos:

            endpoint2timings = _mapSearchInfo2EndpointTimings(search_info)
            endpoint2duration = _mapSearchInfo2EndpointDurations(search_info)
            endpoints = sorted(endpoint2timings.keys() | endpoint2duration.keys())

            for endpoint in endpoints:
                row = [search_info["searchid"], endpoint]

                if endpoint in endpoint2duration:
                    durationInfo = endpoint2duration[endpoint]
                    numRequests = durationInfo["requests"]
                    duration = durationInfo["duration"]
                    if duration is not None:
                        duration = duration.total_seconds()
                    row.extend([numRequests, duration])
                else:
                    row.extend([None] * 2)

                if endpoint in endpoint2timings:
                    timings = endpoint2timings[endpoint]
                    queueTimings = timings["queue"]
                    execTimings = timings["exec"]

                    queueNumTimings = len(queueTimings)
                    queueMinTime = min(queueTimings)
                    queueMaxTime = max(queueTimings)
                    queueMeanTime = statistics.mean(queueTimings)
                    queueStddevTime = (
                        statistics.stdev(queueTimings, queueMeanTime)
                        if queueNumTimings > 1
                        else 0
                    )

                    execNumTimings = len(execTimings)
                    execMinTime = min(execTimings)
                    execMaxTime = max(execTimings)
                    execMeanTime = statistics.mean(execTimings)
                    execStddevTime = (
                        statistics.stdev(execTimings, execMeanTime)
                        if execNumTimings > 1
                        else 0
                    )

                    row.extend(
                        [
                            queueMinTime,
                            queueMaxTime,
                            optional_round(queueMeanTime),
                            optional_round(queueStddevTime),
                            queueNumTimings,
                            execMinTime,
                            execMaxTime,
                            optional_round(execMeanTime),
                            optional_round(execStddevTime),
                            execNumTimings,
                        ]
                    )
                else:
                    row.extend([None] * (5 + 5))

                writer.writerow(row)


def write_search_resource_results_TSV(
    search_infos: Optional[List[SearchInfo]],
    filename: Union[str, int],
    csv_dialect: CSV_DIALECTS = "tsv",
):
    if not search_infos:
        return

    with open(filename, "w", newline="") as fp:
        writer = csv.writer(fp, dialect=csv_dialect)

        fields = [
            # UUID-4 search identifier
            "searchid",
            # endpoint URL
            "endpoint",
            # resource identifier
            "resource",
            # total number of results (-1 means no results)
            "numberOfRecords",
            # number of requests for more results
            "numberOfMoreResultRequests",
            # languages that this resource contains
            "languages",
            # time spent waiting in queue
            "timeQueueMs",
            # time spent executing request
            "timeExecMs",
        ]
        writer.writerow(fields)

        for search_info in search_infos:

            result_records: List[Dict[LogRecordKeyResult, LogRecordValue]] = (
                search_info["result_records_initial"]
            )

            map_resource_search_records: Dict[
                Set[Tuple[str, str]], Dict[LogRecordKeyExplain, LogRecordValue]
            ] = {
                (r["endpoint"], r["resource"]): r
                for r in search_info["explain_records"]
            }

            for record in result_records:

                ep_rid_key: Tuple[str, str] = (record["endpoint"], record["resource"])
                result_more_records: List[Dict[LogRecordKeyResult, LogRecordValue]] = [
                    r
                    for r in search_info["result_records_more"]
                    if (r["endpoint"], r["resource"]) == ep_rid_key
                ]

                if ep_rid_key not in map_resource_search_records:
                    eprint(f"Missing key: {ep_rid_key}")
                    explain_record = {"languages": ["?"]}
                else:
                    explain_record = map_resource_search_records[ep_rid_key]

                writer.writerow(
                    [
                        record["searchid"],
                        record["endpoint"],
                        record["resource"],
                        (
                            record["numberOfRecords"]
                            if "numberOfRecords" in record
                            else -2
                        ),
                        len(result_more_records),
                        "|".join(explain_record["languages"]),
                        record["timeQueueMs"] if "timeQueueMs" in record else None,
                        record["timeExecMs"] if "timeExecMs" in record else None,
                    ]
                )


# --------------------------------------------------------------------------


def parse_loglines(fp_in: io.TextIOWrapper, add_linenumber: bool = True):
    # parse and collect all log records
    records: List[LogRecord] = list()
    errors: List[Tuple[int, str]] = list()
    last_record: Optional[ResourceResultLogRecord] = None

    fp_in_wrapped = enumerate(fp_in, 1)

    for lno, line in fp_in_wrapped:
        try:
            record = parse_logline(line)
            if add_linenumber:
                record["_lno"] = lno

            if last_record is not None:
                if DEBUG:
                    if last_record.get("_lines", -1) <= 1:
                        eprint(
                            f"No multiline found at {lno=}, {line=} for {last_record=}?!"
                        )

                last_record.pop("_lines", None)
                last_record.pop("_is_multiline", None)
                records.append(last_record)
                last_record = None

            if record["type"] == "result" and record.get("_is_multiline", False):
                last_record = record
                last_record["_lines"] = 1

            else:
                records.append(record)
        except Exception as ex:
            # if DEBUG:
            #     eprint(f"Error parsing logline: {lno=}, {line=}, error={ex!s}")
            errors.append((lno, line))

            # try to recover multiline error messages
            if last_record is not None:
                if not is_logline_start(line):
                    try:
                        rest = parse_result_rest(line)

                        error_start = last_record.get("error", "")
                        error_end = rest.get("error", "")
                        error = f"{error_start}\n{error_end}".strip()

                        last_record.update(rest)
                        last_record["error"] = error
                        last_record["_lines"] += 1
                    except Exception as ex:
                        if DEBUG:
                            eprint(
                                f"Error trying to parse result rest logline: {lno=}, {line=}, error={ex!s}"
                            )

    if DEBUG:
        eprint(
            f"Found {len(errors)} / {lno} error lines (possible multiline result log lines with error message)."
        )

    return records


def process_search_infos(
    fp_in: io.TextIOWrapper,
    filter_queryType: Optional[Literal["lex", "cql", "fcs"]] = None,
    filter_hasResults: Optional[bool] = None,
    filter_allResources: Optional[bool] = None,
    filter_requestedMore: Optional[bool] = None,
    filter_endpoint_url: Optional[RegexFilterArg] = None,
    filter_resource_id: Optional[RegexFilterArg] = None,
    transform_map_endpoint_urls_to_domain: bool = False,
    transform_map_endpoint_urls_to_tld: bool = False,
):
    # parse and collect all log records
    records: List[LogRecord] = parse_loglines(fp_in, add_linenumber=DEBUG)

    # filter log records (search/result logs by endpoint/resource)
    records_filtered = filter_records(
        records,
        filter_endpoint_url=filter_endpoint_url,
        filter_resource_id=filter_resource_id,
    )

    if DEBUG:
        eprint(f"Filtered records from {len(records)} to {len(records_filtered)}.")

    logs_were_filtered = len(records_filtered) != len(records)

    records = records_filtered

    # filter and group log records
    grouped_records: GroupedLogRecords = aggregate_records(records)
    # pprint({dtt: len(records) for dtt, records in grouped_records.items()}, stream=sys.stderr)

    # free memory?
    del records

    # precompute and simplify information
    search_infos: Optional[List[SearchInfo]] = build_search_infos(
        grouped_records, logs_were_filtered=logs_were_filtered
    )
    # pprint(search_infos, stream=sys.stderr)

    # free memory?
    del grouped_records

    # search_infos = filter_search_infos(
    #     search_infos,
    #     # - only lex queries
    #     filter_queryType="lex",
    #     # - must not have results
    #     # filter_hasResults=False,
    #     # - only when search on all resources (no restriction/selection by user)
    #     # filter_allResources=True,
    #     # - only when user tried to request more results from search besides initial 10
    #     # filter_requestedMore=True,
    # )

    search_infos_filtered = filter_search_infos(
        search_infos,
        filter_queryType=filter_queryType,
        filter_hasResults=filter_hasResults,
        filter_allResources=filter_allResources,
        filter_requestedMore=filter_requestedMore,
    )

    if DEBUG:
        eprint(
            f"Filtered search_infos from {len(search_infos)} to {len(search_infos_filtered)}."
        )

    search_infos = search_infos_filtered

    # TODO: sanitizing step?
    # see: _mapSearchInfo2EndpointDurations

    search_infos = transform_search_infos(
        search_infos,
        transform_map_endpoint_urls_to_domain=transform_map_endpoint_urls_to_domain,
        transform_map_endpoint_urls_to_tld=transform_map_endpoint_urls_to_tld,
    )

    return search_infos


def main_write(
    search_infos: List[SearchInfo],
    suffix: Optional[str] = None,
):
    if not search_infos:
        return

    # use sys.stdout.fileno() to print to stdout ?

    suffix = f".{suffix}" if suffix else ""

    fn_search = f"searches{suffix}.tsv"
    write_searches_TSV(search_infos, fn_search)
    eprint(f"Wrote searches to {Colors.BROWN}{fn_search}{Colors.END}")

    fn_search_results = f"search_results_by_resource{suffix}.tsv"
    write_search_resource_results_TSV(search_infos, fn_search_results)
    eprint(
        f"Wrote search results per resource to {Colors.BROWN}{fn_search_results}{Colors.END}"
    )

    fn_search_endpoint_timings = f"search_endpoint_timings{suffix}.tsv"
    write_search_endpoint_timings_TSV(search_infos, fn_search_endpoint_timings)
    eprint(
        f"Wrote timing information per search and endpoint to {Colors.BROWN}{fn_search_endpoint_timings}{Colors.END}"
    )


# --------------------------------------------------------------------------


def parse_args(args=None):

    def generate_regex_posneg_type(pos: bool = True):
        def regex_type(value: str) -> RegexFilterArg:
            expr = None
            try:
                expr = re.compile(value)
            except Exception as ex:
                raise ValueError(f"Invalid regex: {ex}")

            return (pos, expr)

        return regex_type

    parser = argparse.ArgumentParser(
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )

    parser.add_argument(
        "--debug",
        action="store_true",
        default=False,
        help="Print some debugging infos to stderr",
    )

    # input file pointer
    parser.add_argument(
        "-i",
        "--input",
        type=argparse.FileType("r"),
        required=True,
        help="Input data (FCS stats logs)",
    )

    # filter
    parser.add_argument(
        "-t",
        "--query-type",
        choices=["cql", "fcs", "lex"],
        default=None,
        help="Filter searches by query type",
    )
    has_results_group = parser.add_mutually_exclusive_group()
    has_results_group.add_argument(
        "-r",
        "--has-results",
        dest="has_results",
        action="store_true",
        default=None,
        help="Filter searches that have results",
    )
    has_results_group.add_argument(
        "-n",
        "--has-no-results",
        dest="has_results",
        action="store_false",
        default=None,
        help="Filter searches that have no results",
    )
    all_resources_group = parser.add_mutually_exclusive_group()
    all_resources_group.add_argument(
        "-a",
        "--all-resources",
        dest="all_resources",
        action="store_true",
        default=None,
        help=(
            "Filter searches that are on all resources"
            " (NOTE: will not work with --endpoint-url-filter[-neg])"
        ),
    )
    all_resources_group.add_argument(
        "-s",
        "--not-all-resources",
        dest="all_resources",
        action="store_false",
        default=None,
        help="Filter searches that are not on all resources",
    )
    requested_more_group = parser.add_mutually_exclusive_group()
    requested_more_group.add_argument(
        "-m",
        "--requested-more",
        dest="requested_more",
        action="store_true",
        default=None,
        help="Filter searches where user requested more results from seom resources",
    )
    requested_more_group.add_argument(
        "-d",
        "--requested-no-more",
        dest="requested_more",
        action="store_false",
        default=None,
        help="Filter searches where user requested no more results besides initial results",
    )

    # more filters
    filter_endpoint_group = parser.add_mutually_exclusive_group()
    filter_endpoint_group.add_argument(
        "--endpoint-url-filter",
        dest="endpoint_url_filter",
        type=generate_regex_posneg_type(True),
        default=None,
        help="Filter for entries matching this endpoint url regex",
    )
    filter_endpoint_group.add_argument(
        "--endpoint-url-filter-neg",
        dest="endpoint_url_filter",
        type=generate_regex_posneg_type(False),
        default=None,
        help="Filter for entries NOT matching this endpoint url regex",
    )
    filter_resource_group = parser.add_mutually_exclusive_group()
    filter_resource_group.add_argument(
        "--resource-id-filter",
        dest="resource_id_filter",
        type=generate_regex_posneg_type(True),
        default=None,
        help="Filter for entries matching this resource id regex",
    )
    filter_resource_group.add_argument(
        "--resource-id-filter-neg",
        dest="resource_id_filter",
        type=generate_regex_posneg_type(False),
        default=None,
        help="Filter for entries NOT matching this resource id regex",
    )

    # transformations
    transform_endpoint_url_group = parser.add_mutually_exclusive_group()
    transform_endpoint_url_group.add_argument(
        "--short-domains",
        action="store_true",
        default=False,
        help="Map Endpoint URL to short domain name format",
    )
    transform_endpoint_url_group.add_argument(
        "--tlds",
        action="store_true",
        default=False,
        help="Map Endpoint URL to TLD",
    )

    subparsers = parser.add_subparsers(dest="mode", required=True)

    parser_write = subparsers.add_parser("write", help="Write statistics into TSV")
    parser_write.add_argument(
        "-l",
        "--name",
        type=str,
        default=None,
        help="Name to include in filename suffix (before file extension)",
    )

    parser_print_searches = subparsers.add_parser(
        "print-searches", help="Print nicely formatted search summary to output"
    )

    parser_print_timings = subparsers.add_parser(
        "print-timings", help="Print nicely formatted timing statistics to output"
    )
    parser_print_timings.add_argument(
        "--summary",
        action="store_true",
        dest="timings_summary",
        help="Print summary",
    )
    parser_print_timings.add_argument(
        "--per-endpoint",
        action="store_true",
        dest="timing_per_endpoint",
        help="Print for each endpoint statistics",
    )
    parser_print_timings.add_argument(
        "--per-search",
        action="store_true",
        dest="timing_per_search",
        help="Print for each search statistics",
    )

    args = parser.parse_args(args=args)

    if DEBUG:
        pprint(args.__dict__, stream=sys.stderr)

    # sanity check
    if args.all_resources is not None and args.endpoint_url_filter is not None:
        raise RuntimeError(
            f"Invalid parameter combination (-[-not]-all-resources and --endpoint-url-filter[-neg]). Results will not be correct!"
        )
    if args.all_resources is not None and args.resource_id_filter is not None:
        raise RuntimeError(
            f"Invalid parameter combination (-[-not]-all-resources and --resource-id-filter[-neg]). Results will not be correct!"
        )

    return args


if __name__ == "__main__":

    args = parse_args()

    if args.debug:
        DEBUG = True

    try:
        search_infos = process_search_infos(
            fp_in=args.input,
            filter_queryType=args.query_type,
            filter_allResources=args.all_resources,
            filter_hasResults=args.has_results,
            filter_requestedMore=args.requested_more,
            filter_endpoint_url=args.endpoint_url_filter,
            filter_resource_id=args.resource_id_filter,
            transform_map_endpoint_urls_to_domain=args.short_domains,
            transform_map_endpoint_urls_to_tld=args.tlds,
        )
    except BrokenPipeError:
        pass

    if args.mode == "write":
        main_write(search_infos=search_infos, suffix=args.name)

    elif args.mode == "print-searches":
        # print pretty info
        print_search_info_report(search_infos=search_infos)

    elif args.mode == "print-timings":
        # print search timing stats
        print_timing_report(
            search_infos=search_infos,
            per_search=args.timing_per_search,
            per_endpoint=args.timing_per_endpoint,
            summary=args.timings_summary,
        )
