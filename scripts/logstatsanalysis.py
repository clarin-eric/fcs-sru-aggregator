#!/usr/bin/env python3

#
# FCS Statistic Log Analysis
# 
# Collates fcsstats log entires about explain/search/result events into search usage information.
# Either pretty print or export as TSVs for futher analysis.
#
# Usage:
#   cat stats.log > ./logstatsanalysis.py
#   ./logstatsanalysis.py < <(zcat stats-2025*.log.gz)
#
#   ./logstatsanalysis.py "alpha" < <( { zcat alpha/stats-2025-*.log.gz ; cat alpha/stats.log ; } )
#   ./logstatsanalysis.py "prod" < <( { zcat prod/stats-2025-*.log.gz ; cat prod/stats.log ; } )
#


import csv
import io
import sys
from datetime import timedelta, datetime
from dataclasses import dataclass
from pprint import pprint
from typing import Dict, List, Literal, Optional, Set, Tuple, Union


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


# --------------------------------------------------------------------------


LogRecordKeyGeneric = Literal["date", "time", "type"]
LogRecordKeyEndpointResource = Literal["endpoint", "resource"]
LogRecordKeySearchID = Literal["searchid"]
LogRecordKeyExplain = Union[
    LogRecordKeyEndpointResource, Literal["languages", "capabilities"]
]
LogRecordKeySearchInitial = Literal["queryType", "query", "language"]
LogRecordKeySearchResource = Union[
    LogRecordKeyEndpointResource,
    Literal["sruversion", "batch_start", "batch_len", "queryType", "query"],
]
LogRecordKeySearch = Union[
    LogRecordKeySearchID, LogRecordKeySearchInitial, LogRecordKeySearchResource
]
LogRecordKeyResult = Union[
    LogRecordKeySearchID,
    LogRecordKeyEndpointResource,
    Literal["numberOfRecords", "nextRecord"],
]
LogRecordKey = Union[
    LogRecordKeyGeneric, LogRecordKeyExplain, LogRecordKeySearch, LogRecordKeyResult
]
LogRecordValue = Union[str, List[str], int]
LogRecord = Dict[LogRecordKey, LogRecordValue]
LogRecordList = List[LogRecord]


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
        assert idx_end == len(text) - 1, f"{idx_end=} {len(text)-1=} {text=}"

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


def parse_explain(msg: str) -> Dict[LogRecordKeyExplain, Union[str, List[str]]]:
    endpoint, msg = chop_quoted_value(msg, "endpoint")
    resource, msg = chop_quoted_value(msg, "resource")
    languages, msg = chop_quoted_value(msg, "languages", quotes=("[", "]"))
    capabilities, msg = chop_quoted_value(
        msg, "capabilities", quotes=("[", "]"), space_after=False
    )

    return {
        "endpoint": endpoint,
        "resource": resource,
        "languages": languages.split(", "),
        "capabilities": capabilities.split(", "),
    }


def parse_search_resource(
    msg: str,
) -> Dict[LogRecordKeySearchResource, Union[str, int]]:
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


def parse_search_initial(msg: str) -> Dict[LogRecordKeySearchInitial, str]:
    queryType, msg = chop_quoted_value(msg, "queryType")
    language, msg = chop_quoted_value(msg, "language", from_end=True)

    needle = "query='"
    assert msg.startswith(needle) and msg.endswith("'")
    query = msg[len(needle) : -1]

    return {"queryType": queryType, "query": query, "language": language}


def parse_search(msg: str) -> Dict[LogRecordKeySearch, Union[str, int]]:
    searchid, msg = chop_quoted_value(msg, None, quotes=("[", "]"))

    if msg.startswith("endpoint='"):
        return {"searchid": searchid, **parse_search_resource(msg)}

    if msg.startswith("queryType='"):
        return {"searchid": searchid, **parse_search_initial(msg)}

    return {"searchid": searchid}


def parse_result(msg: str) -> Dict[LogRecordKeyResult, Union[str, int]]:
    searchid, msg = chop_quoted_value(msg, None, quotes=("[", "]"))
    endpoint, msg = chop_quoted_value(msg, "endpoint")
    resource, msg = chop_quoted_value(msg, "resource")
    numberOfRecords, msg = chop_quoted_value(msg, "numberOfRecords", quotes=None)
    nextRecord, msg = chop_quoted_value(
        msg, "nextRecord", quotes=None, space_after=False
    )

    return {
        "searchid": searchid,
        "endpoint": endpoint,
        "resource": resource,
        "numberOfRecords": int(numberOfRecords),
        "nextRecord": int(nextRecord),
    }


def parse_logmsg(type: str, msg: str) -> LogRecord:
    if type == "explain":
        return parse_explain(msg)

    if type == "search":
        return parse_search(msg)

    if type == "result":
        return parse_result(msg)

    assert False


def parse_logline(line: str) -> LogRecord:
    line = line.rstrip()

    # strip log level
    _loglevel, line = chop_quoted_value(line, None, quotes=None)
    # strip variable padding
    line = line.lstrip()

    logdatetime, line = chop_quoted_value(line, None, quotes=("[", "]"))
    logdatetime_parsed = datetime.strptime(logdatetime, "%Y-%m-%d %H:%M:%S,%f")
    logdatetime_parsed = logdatetime_parsed.replace(microsecond=0)
    logdate, logtime = logdatetime.split(" ", 1)

    logger, msg = line.split(": ", 1)
    type = logger.split(".", 1)[-1]

    data = {
        "date": logdate,
        "time": logtime,
        # "date_p": logdatetime_parsed.date(),
        # "time_p": logdatetime_parsed.time(),
        "type": type,
        # process message
        **parse_logmsg(type, msg),
    }

    return data


# --------------------------------------------------------------------------


DateTimeTuple = Tuple[str, str]
DateTimeTypeTuple = Tuple[DateTimeTuple, Literal["explain", "search/result"]]
GroupedLogRecords = Dict[DateTimeTypeTuple, LogRecordList]

InitialSearchRequestLogRecord = Dict[
    Union[
        LogRecordKeyGeneric,
        LogRecordKeySearchID,
        LogRecordKeySearchInitial,
    ],
    LogRecordValue,
]

SearchInfoKeys = Union[
    # str
    Literal["date", "time", "searchid", "queryType", "query"],
    # int
    Literal[
        "resources_available",
        "resources_searched",
        "resources_answered",
        "resources_searched_further",
        "requests_for_more_results",
        "result_hits_total",
    ],
    # List[str]
    Literal["resources_searched_languages"],
    # LogRecord, LogRecordList
    Literal[
        "explain_records",
        "search_record",
        "search_resource_records",
        "result_records_initial",
        "result_records_more",
    ],
]
SearchInfo = Dict[SearchInfoKeys, Union[str, int, List[str], LogRecord, LogRecordList]]
SearchInfoList = List[SearchInfo]


def aggregate_records(
    records: LogRecordList,
    threshold_explain_run: timedelta = timedelta(minutes=15),
) -> GroupedLogRecords:
    # aggregate search + result by searchid
    # order explain and insert first aggregated searchid by date

    map_searchid_records: Dict[str, LogRecordList] = dict()
    list_explain_records: LogRecordList = list()

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

    map_dt_explain_records: Dict[DateTimeTuple, LogRecordList] = dict()
    map_searchid_earliest: Dict[str, DateTimeTuple] = dict()

    # everything should usually already be sorted (except if multiple log files are merge in random order)
    def fn_get_dt(record: LogRecord) -> DateTimeTuple:
        return (record["date"], record["time"])

    # convenience method
    def fn_record_to_dt(record: LogRecord):
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

    map_dt_with_type_records: Dict[DateTimeTypeTuple, LogRecordList] = dict()

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


def build_search_infos(grouped_records: GroupedLogRecords) -> Optional[SearchInfoList]:
    if not grouped_records:
        return None

    def count_dedup_by_resource(records: LogRecordList) -> int:
        uniquer: Set[Tuple[str, str]] = {
            (r["endpoint"], r["resource"]) for r in records
        }
        return len(uniquer)

    def get_initial_result_records_by_resource(
        records: LogRecordList,
    ) -> Tuple[LogRecordList, LogRecordList]:
        # group by endpoint/resource
        map_resource_records: Dict[Tuple[str, str], LogRecordList] = dict()
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
        first_records: LogRecordList = list()
        more_records: LogRecordList = list()
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

    list_search_info: SearchInfoList = list()

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
            # TODO: do we need to assert that there is only ever one?
            assert (
                search_request is not None
            ), f"must have initial search request! {dt=} {type=}"

            # ------------------------
            # explain --> available resources

            # compute the list of available resources based on query type
            num_avail_resources: int = 0
            explain_records_for_capability: LogRecordList = list()

            if last_explain:
                explain_records = grouped_records[last_explain]
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
            assert len(search_resources_records) > 0, "must have search request records"
            search_resources_records_initial = [
                r for r in search_resources_records if r["batch_start"] == 1
            ]
            search_resources_records_more = [
                r for r in search_resources_records if r["batch_start"] > 1
            ]

            num_used_resources: int = len(search_resources_records_initial)
            num_requests_for_more_results: int = len(search_resources_records_more)

            set_resource_search_records: Set[Tuple[str, str]] = {
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
            result_counts = [
                r["numberOfRecords"]
                for r in result_records_firsts
                if r["numberOfRecords"] != -1
            ]

            num_result_count_total = sum(result_counts)

            # ------------------------

            search_info: SearchInfo = {
                # copy all relevant information about search
                **{
                    key: value
                    for key, value in search_request.items()
                    if key in ("date", "time", "searchid", "queryType", "query")
                },
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
                "search_resource_records": search_resources_records,
                "result_records_initial": result_records_firsts,
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


# --------------------------------------------------------------------------


def print_search_info_report(
    search_infos: Optional[SearchInfoList],
    filter_queryType: Optional[Literal["lex", "cql", "fcs"]] = None,
    filter_hasResults: Optional[bool] = None,
    filter_allResources: Optional[bool] = None,
):
    if not search_infos:
        return

    for search_info in search_infos:
        # filter / output control
        if filter_queryType is not None:
            if search_info["queryType"] != filter_queryType:
                continue
        if filter_allResources is not None:
            has_used_all_resources = (
                search_info["resources_available"] == search_info["resources_searched"]
            )
            if has_used_all_resources != filter_allResources:
                continue
        if filter_hasResults is not None:
            has_results = search_info["result_hits_total"]
            if has_results != filter_hasResults:
                continue

        print("-" * 70)

        print(f"Search ID: {Colors.GREEN}{search_info['searchid']}{Colors.END}")
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
                if r["numberOfRecords"] != -1
            ]

            print(
                f"  --> got {Colors.LIGHT_GREEN}{search_info['result_hits_total']}{Colors.END} total results"
                f" in {Colors.LIGHT_RED}{len(result_counts)}{Colors.END} resources"
            )
            if result_counts and len(result_counts) > 1:
                print(
                    f"          = {Colors.DARK_GRAY}{(Colors.END+'+'+Colors.DARK_GRAY).join(map(str, result_counts))}{Colors.END}"
                )


def print_report(
    grouped_records: GroupedLogRecords,
    filter_queryType: Optional[Literal["lex", "cql", "fcs"]] = None,
    filter_hasResults: Optional[bool] = None,
    filter_allResources: Optional[bool] = None,
):
    if not grouped_records:
        return

    last_explain: Optional[DateTimeTypeTuple] = None
    last_explain_num_resources: int = 0
    for (dt, type), records in grouped_records.items():
        if type == "explain":
            last_explain = (dt, type)
            # there should only ever be explain records (without duplicates) due to grouping and type
            last_explain_num_resources = len(records)
            continue

        if type == "search/result":
            search_request: Optional[InitialSearchRequestLogRecord] = next(
                (r for r in records if r["type"] == "search" and "endpoint" not in r),
                None,
            )
            assert (
                search_request is not None
            ), f"must have initial search request! {dt=} {type=}"

            # NOTE: we can do filtering here...
            if filter_queryType:
                if search_request["queryType"] != filter_queryType:
                    continue

            # NOTE: we might want to update the list of available resources based on query type
            num_avail_resources = last_explain_num_resources
            # if search_request["queryType"] != "cql":
            if last_explain:
                explain_records = grouped_records[last_explain]
                wanted_capability = "BASIC_SEARCH"
                if search_request["queryType"] == "lex":
                    wanted_capability = "LEX_SEARCH"
                elif search_request["queryType"] == "fcs":
                    wanted_capability = "ADVANCED_SEARCH"
                explain_records_qt = [
                    r for r in explain_records if wanted_capability in r["capabilities"]
                ]
                num_avail_resources = len(explain_records_qt)

            search_resources_records = [
                r for r in records if r["type"] == "search" and "endpoint" in r
            ]
            search_resources_records_initial = [
                r for r in search_resources_records if r["batch_start"] == 1
            ]
            search_resources_records_more = [
                r for r in search_resources_records if r["batch_start"] > 1
            ]

            result_records = [r for r in records if r["type"] == "result"]
            # TODO: do proper dedup on endpoint+resource
            result_records_first = [
                r for r in result_records if r["nextRecord"] in (-1, 11)
            ]
            result_counts = [
                r["numberOfRecords"]
                for r in result_records_first
                if r["numberOfRecords"] != -1
            ]

            if filter_allResources is not None:
                used_all_resources = (
                    len(search_resources_records_initial) == num_avail_resources
                )
                if used_all_resources != filter_allResources:
                    continue

            if filter_hasResults is not None:
                has_results = sum(result_counts) > 0
                if has_results != filter_hasResults:
                    continue

            print("-" * 70)
            print(f"Search ID: {Colors.GREEN}{search_request['searchid']}{Colors.END}")
            print(
                f"  on {Colors.GREEN}{search_request['date']}{Colors.END}"
                f" at {Colors.GREEN}{search_request['time']}{Colors.END}"
            )
            print(
                f"  queryType = {Colors.LIGHT_CYAN}{search_request['queryType']}{Colors.END}"
                f" query = >>{Colors.LIGHT_BLUE}{search_request['query']}{Colors.END}<<"
            )
            print(
                f"  search through {Colors.RED}{len(search_resources_records_initial):3d}{Colors.END}"
                f" / {Colors.RED}{num_avail_resources:3d}{Colors.END} resources"
            )
            if search_resources_records_more:
                print(
                    f"    requested {Colors.LIGHT_RED}{len(search_resources_records_more)}{Colors.END} more results"
                )
            if sum(result_counts):
                print(
                    f"  --> got {Colors.LIGHT_GREEN}{sum(result_counts)}{Colors.END} total results"
                    f" in {Colors.LIGHT_RED}{len(result_counts)}{Colors.END} resources"
                )
                if result_counts and len(result_counts) > 1:
                    print(
                        f"          = {Colors.DARK_GRAY}{(Colors.END+'+'+Colors.DARK_GRAY).join(map(str, result_counts))}{Colors.END}"
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
    search_infos: Optional[SearchInfoList],
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
                ]
            )


def write_search_resource_results_TSV(
    search_infos: Optional[SearchInfoList],
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
                explain_record = map_resource_search_records[ep_rid_key]

                writer.writerow(
                    [
                        record["searchid"],
                        record["endpoint"],
                        record["resource"],
                        record["numberOfRecords"],
                        len(result_more_records),
                        "|".join(explain_record["languages"]),
                    ]
                )


# --------------------------------------------------------------------------


def main(fp_in: io.TextIOWrapper, suffix: Optional[str] = None):
    # parse and collect all log records
    records: LogRecordList = list()
    for line in fp_in:
        record = parse_logline(line)
        records.append(record)

    # filter and group log records
    grouped_records: GroupedLogRecords = aggregate_records(records)
    # pprint({dtt: len(records) for dtt, records in grouped_records.items()})

    # precompute and simplify information
    search_infos: Optional[SearchInfoList] = build_search_infos(grouped_records)
    # pprint(search_infos)

    # print pretty info
    # print_search_info_report(
    #     search_infos,
    #     # - only lex queries
    #     filter_queryType="lex",
    #     # - must not have results
    #     filter_hasResults=False,
    #     # - only when search on all resources (no restriction/selection by user)
    #     filter_allResources=True,
    # )

    if not search_infos:
        return

    # filter for lex only
    search_infos = [si for si in search_infos if si["queryType"] == "lex"]

    # use sys.stdout.fileno() to print to stdout
    suffix = f".{suffix}" if suffix else ""
    write_searches_TSV(search_infos, f"searches{suffix}.tsv")
    write_search_resource_results_TSV(
        search_infos, f"search_results_by_resource{suffix}.tsv"
    )


if __name__ == "__main__":

    suffix = next(iter(sys.argv[1:]), None)

    fp_in = sys.stdin

    try:
        main(fp_in, suffix)
    except BrokenPipeError:
        pass

