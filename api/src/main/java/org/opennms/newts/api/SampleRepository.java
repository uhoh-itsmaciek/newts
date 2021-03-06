/*
 * Copyright 2015, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.api;


import java.util.Collection;

import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.base.Optional;


public interface SampleRepository {

    /**
     * Query measurements.
     *
     * @param context
     *            context to query
     * @param resource
     *            name of the sampled resource
     * @param start
     *            query start time (defaults to 24 hours less than {@code end}, if absent)
     * @param end
     *            query end time (defaults to current time if absent)
     * @param descriptor
     *            aggregation descriptor
     * @param resolution
     *            temporal resolution of results (defaults to a value resulting in 1-10 measurements, if absent)
     * @return query results
     */
    public Results<Measurement> select(Context context, Resource resource, Optional<Timestamp> start, Optional<Timestamp> end, ResultDescriptor descriptor, Optional<Duration> resolution);

    /**
     * Query measurements.
     *
     * @param context
     *            context to query
     * @param resource
     *            name of the sampled resource
     * @param start
     *            query start time (defaults to 24 hours less than {@code end}, if absent)
     * @param end
     *            query end time (defaults to current time if absent)
     * @param descriptor
     *            aggregation descriptor
     * @param resolution
     *            temporal resolution of results (defaults to a value resulting in 1-10 measurements, if absent)
     * @param callback
     *            callback
     * @return query results
     */
    public Results<Measurement> select(Context context, Resource resource, Optional<Timestamp> start, Optional<Timestamp> end, ResultDescriptor descriptor, Optional<Duration> resolution, SampleSelectCallback callback);

    /**
     * Read stored samples.
     *
     * @param context
     *            context to query
     * @param resource
     *            name of the sampled resource
     * @param start
     *            query start time (defaults to 24 hours less than {@code end}, if absent)
     * @param end
     *            query end time (defaults to current time if absent)
     * @return query results
     */
    public Results<Sample> select(Context context, Resource resource, Optional<Timestamp> start, Optional<Timestamp> end);

    /**
     * Write (store) samples.
     *
     * @param samples
     *            samples to insert
     */
    public void insert(Collection<Sample> samples);

    /**
     * Write (store) samples.
     *
     * @param samples
     *            samples to insert
     * @param calculateTimeToLive
     *            true if the effective TTL should be calculated using the sample timestamps
     */
    public void insert(Collection<Sample> samples, boolean calculateTimeToLive);

    /**
     * Delete stored samples.
     *
     * @param context context to delete from
     * @param resource name of the resource to delete from
     */
    public void delete(Context context, Resource resource);

}
