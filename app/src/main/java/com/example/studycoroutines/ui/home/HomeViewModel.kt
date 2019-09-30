package com.example.studycoroutines.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import com.example.studycoroutines.network.GitHubService
import com.example.studycoroutines.network.RequestData
import com.example.studycoroutines.network.User
import com.example.studycoroutines.network.createGitHubService
import retrofit2.Response

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text
    private var loadingCoroutineJob: Job? = null

    suspend fun loadContributorsSuspend(service: GitHubService, req: RequestData): List<User> {
        val repos = service
            .getOrgRepos(req.org)
            .body() ?: listOf()
        Log.d("DEBUG2", "repos are loaded: size = ${repos.size}")
        return repos.map { repo ->
            CoroutineScope(Dispatchers.IO).async {
                val bodyList = service
                    .getRepoContributors(req.org, repo.name)
                    .bodyList()
                Log.d("DEBUG2", "contributors for ${repo.name} are loaded: size = ${bodyList.size}")
                bodyList
            }
        }.awaitAll().aggregate()
    }

    fun loadContributors(username: String, password: String, org: String = "kotlin") {
        val req = RequestData(username, password, org)
        val service = createGitHubService(req.username, req.password)
        CoroutineScope(Dispatchers.Main).launch {
            loadingCoroutineJob?.apply {
                Log.d("DEBUG1", "job: $this.cancelAndJoin()")
                cancelAndJoin()
            }
            _text.value = "Cleared"
            _text.value = CoroutineScope(Dispatchers.Default).async {
                val users = loadContributorsSuspend(service, req)
                updateContributors(users)
            }.apply {
                loadingCoroutineJob = this
                Log.d("DEBUG1", "started: $this")
            }.await()
            Log.d("DEBUG1", "finished: $loadingCoroutineJob")
        }
    }

    private fun updateContributors(users: List<User>): String = StringBuilder().apply {
        users.map {
            arrayOf(it.login, it.contributions)
        }.toTypedArray().iterator().forEach {
            appendln("${it[0]}: ${it[1]}")
        }
    }.toString()

    fun <T> Response<List<T>>.bodyList(): List<T> {
        return body() ?: listOf()
    }

    fun List<List<User>>.aggregate(): List<User> =
        flatten().groupBy { it.login }.map { (login, group) ->
            User(login, group.sumBy { it.contributions })
        }.sortedByDescending { it.contributions }
}