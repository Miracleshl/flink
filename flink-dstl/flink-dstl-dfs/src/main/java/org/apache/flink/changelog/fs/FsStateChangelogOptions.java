/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.changelog.fs;

import org.apache.flink.annotation.Experimental;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.MemorySize;

import java.time.Duration;

/** {@link ConfigOptions} for {@link FsStateChangelogStorage}. */
@Experimental
public class FsStateChangelogOptions {

    public static final ConfigOption<String> BASE_PATH =
            ConfigOptions.key("state.changelog.dstl.dfs.base-path")
                    .stringType()
                    .noDefaultValue()
                    .withDeprecatedKeys("dstl.dfs.base-path")
                    .withDescription("Base path to store changelog files.");

    public static final ConfigOption<Boolean> COMPRESSION_ENABLED =
            ConfigOptions.key("state.changelog.dstl.dfs.compression.enabled")
                    .booleanType()
                    .defaultValue(false)
                    .withDeprecatedKeys("dstl.dfs.compression.enabled")
                    .withDescription("Whether to enable compression when serializing changelog.");

    public static final ConfigOption<MemorySize> PREEMPTIVE_PERSIST_THRESHOLD =
            ConfigOptions.key("state.changelog.dstl.dfs.preemptive-persist-threshold")
                    .memoryType()
                    .defaultValue(MemorySize.parse("5MB"))
                    .withDeprecatedKeys("dstl.dfs.preemptive-persist-threshold")
                    .withDescription(
                            "Size threshold for state changes of a single operator "
                                    + "beyond which they are persisted pre-emptively without waiting for a checkpoint. "
                                    + " Improves checkpointing time by allowing quasi-continuous uploading of state changes "
                                    + "(as opposed to uploading all accumulated changes on checkpoint).");

    public static final ConfigOption<Duration> PERSIST_DELAY =
            ConfigOptions.key("state.changelog.dstl.dfs.batch.persist-delay")
                    .durationType()
                    .defaultValue(Duration.ofMillis(10))
                    .withDeprecatedKeys("dstl.dfs.batch.persist-delay")
                    .withDescription(
                            "Delay before persisting changelog after receiving persist request (on checkpoint). "
                                    + "Minimizes the number of files and requests "
                                    + "if multiple operators (backends) or sub-tasks are using the same store. "
                                    + "Correspondingly increases checkpoint time (async phase).");

    public static final ConfigOption<MemorySize> PERSIST_SIZE_THRESHOLD =
            ConfigOptions.key("state.changelog.dstl.dfs.batch.persist-size-threshold")
                    .memoryType()
                    .defaultValue(MemorySize.parse("10MB"))
                    .withDeprecatedKeys("dstl.dfs.batch.persist-size-threshold")
                    .withDescription(
                            "Size threshold for state changes that were requested to be persisted but are waiting for "
                                    + PERSIST_DELAY.key()
                                    + " (from all operators). "
                                    + ". Once reached, accumulated changes are persisted immediately. "
                                    + "This is different from "
                                    + PREEMPTIVE_PERSIST_THRESHOLD.key()
                                    + " as it happens AFTER the checkpoint and potentially for state changes of multiple operators. "
                                    + "Must not exceed in-flight data limit (see below)");

    public static final ConfigOption<MemorySize> UPLOAD_BUFFER_SIZE =
            ConfigOptions.key("state.changelog.dstl.dfs.upload.buffer-size")
                    .memoryType()
                    .defaultValue(MemorySize.parse("1MB"))
                    .withDeprecatedKeys("dstl.dfs.upload.buffer-size")
                    .withDescription("Buffer size used when uploading change sets");

    public static final ConfigOption<Integer> NUM_UPLOAD_THREADS =
            ConfigOptions.key("state.changelog.dstl.dfs.upload.num-threads")
                    .intType()
                    .defaultValue(5)
                    .withDeprecatedKeys("dstl.dfs.upload.num-threads")
                    .withDescription("Number of threads to use for upload.");

    public static final ConfigOption<Integer> NUM_DISCARD_THREADS =
            ConfigOptions.key("state.changelog.dstl.dfs.discard.num-threads")
                    .intType()
                    .defaultValue(1)
                    .withDeprecatedKeys("dstl.dfs.discard.num-threads")
                    .withDescription(
                            "Number of threads to use to discard changelog (e.g. pre-emptively uploaded unused state).");

    public static final ConfigOption<MemorySize> IN_FLIGHT_DATA_LIMIT =
            ConfigOptions.key("state.changelog.dstl.dfs.upload.max-in-flight")
                    .memoryType()
                    .defaultValue(MemorySize.parse("100MB"))
                    .withDeprecatedKeys("dstl.dfs.upload.max-in-flight")
                    .withDescription(
                            "Max amount of data allowed to be in-flight. "
                                    + "Upon reaching this limit the task will be back-pressured. "
                                    + " I.e., snapshotting will block; normal processing will block if "
                                    + PREEMPTIVE_PERSIST_THRESHOLD.key()
                                    + " is set and reached. "
                                    + "The limit is applied to the total size of in-flight changes if multiple "
                                    + "operators/backends are using the same changelog storage. "
                                    + "Must be greater than or equal to "
                                    + PERSIST_SIZE_THRESHOLD.key());

    public static final ConfigOption<String> RETRY_POLICY =
            ConfigOptions.key("state.changelog.dstl.dfs.upload.retry-policy")
                    .stringType()
                    .defaultValue("fixed")
                    .withDeprecatedKeys("dstl.dfs.upload.retry-policy")
                    .withDescription(
                            "Retry policy for the failed uploads (in particular, timed out). Valid values: none, fixed.");
    public static final ConfigOption<Duration> UPLOAD_TIMEOUT =
            ConfigOptions.key("state.changelog.dstl.dfs.upload.timeout")
                    .durationType()
                    .defaultValue(Duration.ofSeconds(1))
                    .withDeprecatedKeys("dstl.dfs.upload.timeout")
                    .withDescription(
                            "Time threshold beyond which an upload is considered timed out. "
                                    + "If a new attempt is made but this upload succeeds earlier then this upload result will be used. "
                                    + "May improve upload times if tail latencies of upload requests are significantly high. "
                                    + "Only takes effect if "
                                    + RETRY_POLICY.key()
                                    + " is fixed. "
                                    + "Please note that timeout * max_attempts should be less than "
                                    + CheckpointingOptions.CHECKPOINTING_TIMEOUT.key());
    public static final ConfigOption<Integer> RETRY_MAX_ATTEMPTS =
            ConfigOptions.key("state.changelog.dstl.dfs.upload.max-attempts")
                    .intType()
                    .defaultValue(3)
                    .withDeprecatedKeys("dstl.dfs.upload.max-attempts")
                    .withDescription(
                            "Maximum number of attempts (including the initial one) to perform a particular upload. "
                                    + "Only takes effect if "
                                    + RETRY_POLICY.key()
                                    + " is fixed.");
    public static final ConfigOption<Duration> RETRY_DELAY_AFTER_FAILURE =
            ConfigOptions.key("state.changelog.dstl.dfs.upload.next-attempt-delay")
                    .durationType()
                    .defaultValue(Duration.ofMillis(500))
                    .withDeprecatedKeys("dstl.dfs.upload.next-attempt-delay")
                    .withDescription(
                            "Delay before the next attempt (if the failure was not caused by a timeout).");

    public static final ConfigOption<Duration> CACHE_IDLE_TIMEOUT =
            ConfigOptions.key("state.changelog.dstl.dfs.download.local-cache.idle-timeout-ms")
                    .durationType()
                    .defaultValue(Duration.ofMinutes(10))
                    .withDeprecatedKeys("dstl.dfs.download.local-cache.idle-timeout-ms")
                    .withDescription(
                            "Maximum idle time for cache files of distributed changelog file, "
                                    + "after which the cache files will be deleted.");
}
