package eu.clarin.sru.fcs.aggregator.app.version;

public interface VersionInfo {
    // https://github.com/git-commit-id/git-commit-id-plugin-core/blob/master/src/main/java/pl/project13/core/GitCommitPropertyConstant.java
    // https://github.com/git-commit-id/git-commit-id-maven-plugin/blob/master/docs/access-version-info-at-runtime.md

    /**
     * Represents the tag on the current commit. Similar to running
     * 
     * <pre>
     *     git tag --points-at HEAD
     * </pre>
     */
    String TAG = "${git.tag}";
    /**
     * Represents a list of tags which contain the specified commit.
     * Similar to running
     * 
     * <pre>
     *     git tag --contains
     * </pre>
     */
    String TAGS = "${git.tags}";
    /**
     * Represents the current branch name. Falls back to commit-id for detached
     * HEAD.
     *
     * Note: When an user uses the {@code evaluateOnCommit} property to gather the
     * branch for an arbitrary commit (really anything besides the default
     * {@code HEAD}) this plugin will perform a {@code git branch --points-at} which
     * might return a comma separated list of branch names that points to the
     * specified commit.
     */
    String BRANCH = "${git.branch}";
    /**
     * A working tree is said to be "dirty" if it contains modifications which have
     * not been committed to the current branch.
     */
    String DIRTY = "${git.dirty}";
    /**
     * Represents the URL of the remote repository for the current git project.
     */
    String REMOTE_ORIGIN_URL = "${git.remote.origin.url}";

    /**
     * Represents the commit’s SHA-1 hash. Note this is exchangeable with the
     * git.commit.id property and might not be exposed. See
     * {@code commitIdGenerationMode}.
     */
    String COMMIT_ID = "${git.commit.id.full}";
    /**
     * Represents the abbreviated (shorten version) commit hash.
     */
    String COMMIT_ID_ABBREV = "${git.commit.id.abbrev}";
    /**
     * Represents an object a human readable name based on a the commit
     * (provides {@code git describe} for the given commit).
     */
    String DESCRIBE = "${git.commit.id.describe}";
    /**
     * Represents the same value as git.commit.id.describe, just with the git hash
     * part removed (the abbreviated git commit hash part from
     * {@code git describe}).
     */
    String DESCRIBE_SHORT = "${git.commit.id.describe-short}";
    /**
     * Represents the user name of the user who performed the commit.
     */
    String COMMIT_USER_NAME = "${git.commit.user.name}";
    /**
     * Represents the user eMail of the user who performed the commit.
     */
    String COMMIT_USER_EMAIL = "${git.commit.user.email}";
    /**
     * Represents the subject of the commit message - may <b>not</b> be suitable for
     * filenames. Similar to running
     * 
     * <pre>
     *     git log -1 --pretty=format:%s
     * </pre>
     */
    String COMMIT_MESSAGE_SHORT = "${git.commit.message.short}";
    /**
     * Represents the (formatted) time stamp when the commit has been performed.
     */
    String COMMIT_TIME = "${git.commit.time}";
    /**
     * Represents the name of the closest available tag.
     * The closest tag may depend on your git describe config that may or may not
     * take lightweight tags into consideration.
     */
    String CLOSEST_TAG_NAME = "${git.closest.tag.name}";
    /**
     * Represents the number of commits to the closest available tag.
     * The closest tag may depend on your git describe config that may or may not
     * take lightweight tags into consideration.
     */
    String CLOSEST_TAG_COMMIT_COUNT = "${git.closest.tag.commit.count}";

    /**
     * Represents the git user name that is configured where the properties have
     * been generated.
     */
    String BUILD_USER_NAME = "${git.build.user.name}";
    /**
     * Represents the git user eMail that is configured where the properties have
     * been generated.
     */
    String BUILD_USER_EMAIL = "${git.build.user.email}";
    /**
     * Represents the (formatted) timestamp when the last build was executed. If
     * written to the git.properties file represents the latest build time when that
     * file was written / updated.
     */
    String BUILD_TIME = "${git.build.time}";
    /**
     * Represents the project version of the current project.
     */
    String BUILD_VERSION = "${git.build.version}";

}
