/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.example.paging.pagingwithnetwork.reddit.repository.inMemory.byPage

import androidx.annotation.MainThread
import androidx.lifecycle.Transformations.switchMap
import androidx.lifecycle.asLiveData
import androidx.paging.PagingConfig
import androidx.paging.PagingDataFlow
import com.android.example.paging.pagingwithnetwork.reddit.api.RedditApi
import com.android.example.paging.pagingwithnetwork.reddit.repository.Listing
import com.android.example.paging.pagingwithnetwork.reddit.repository.RedditPostRepository
import com.android.example.paging.pagingwithnetwork.reddit.vo.RedditPost
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor

/**
 * Repository implementation that returns a Listing that loads data directly from network by using
 * the previous / next page keys returned in the query.
 */
class InMemoryByPageKeyRepository(
        private val redditApi: RedditApi,
        private val networkExecutor: Executor
) : RedditPostRepository {
    @MainThread
    override fun postsOfSubreddit(subReddit: String, pageSize: Int): Listing<RedditPost> {
        val sourceFactory = SubRedditDataSourceFactory(
                redditApi = redditApi,
                subredditName = subReddit,
                networkDispatcher = networkExecutor.asCoroutineDispatcher()
        )

        val refreshState = switchMap(sourceFactory.sourceLiveData) { it.initialLoad }

        val pagedDataFlow = LivePagingData(
                config = PagingConfig(pageSize = pageSize),
                pagingSourceFactory = sourceFactory
        )

        return Listing(
                pagedData = pagedDataFlow.asLiveData(),
                networkState = switchMap(sourceFactory.sourceLiveData) { it.networkState },
                refresh = { sourceFactory.sourceLiveData.value?.invalidate() },
                refreshState = refreshState
        )
    }
}

