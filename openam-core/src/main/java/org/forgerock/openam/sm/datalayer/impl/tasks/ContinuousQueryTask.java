/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.sm.datalayer.impl.tasks;

import java.text.MessageFormat;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.continuous.ContinuousQuery;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;

/**
 * A task representing a continuous query that - once execution has begun on the data layer - will persist and
 * continually pass results back to the supplied listener.
 *
 * Results which match the supplied filter will be pushed up the {@link Connection} to the
 * {@link ContinuousQueryListener}.
 *
 * @see ContinuousQueryListener
 * @see TokenFilter
 */
public class ContinuousQueryTask implements Task {

    private final ContinuousQueryListener listener;
    private final TokenFilter tokenFilter;

    private PromiseImpl<ContinuousQuery, NeverThrowsException> queryPromise;

    /**
     * Generate a new {@link ContinuousQueryTask} to be applied to the underlying datastore using the
     * configured {@link TokenFilter}.
     *
     * @param tokenFilter The filter to be continually applied in the datastore layer.
     * @param listener Results matching the {@link TokenFilter} will be sent to this listener.
     */
    public ContinuousQueryTask(TokenFilter tokenFilter, ContinuousQueryListener listener) {
        this.listener = listener;
        this.tokenFilter = tokenFilter;
        this.queryPromise = PromiseImpl.create();
    }

    /**
     * Retrieve the query promise which this task generated.
     *
     * This will only contain a valid {@link ContinuousQuery} once {@code execute} has been called.
     *
     * @return The {@link ContinuousQuery} generated by this task, via an originally generated {@link Promise}.
     */
    public Promise<ContinuousQuery, NeverThrowsException> getQuery() {
        return queryPromise;
    }

    @Override
    public void execute(TokenStorageAdapter adapter) {
        queryPromise.tryHandleResult(adapter.startContinuousQuery(tokenFilter, listener));
    }

    @Override
    public void processError(DataLayerException error) {
        listener.processError(error);
    }

    public String toString() {
        return MessageFormat.format("ContinuousQueryTask: {0}", tokenFilter);
    }
}
