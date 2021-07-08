package com.athanasioua.battleship.model.newp.viewmodel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.athanasioua.battleship.R
import com.athanasioua.battleship.model.newp.Utils
import com.athanasioua.battleship.model.newp.model.*
import com.athanasioua.battleship.model.newp.view.DisplayActivity
import com.athanasioua.battleship.model.newp.view.Login
import com.google.firebase.database.core.Repo
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class EncDec : ViewModel() {
    private val publicIndex: MutableLiveData<Indexing?> = MutableLiveData()
    private val displayItemsList: MutableLiveData<ArrayList<DisplayItem>?> = MutableLiveData()
    private val activeRepository: MutableLiveData<ActiveRepository?> = MutableLiveData()
    private val breadcrumb: MutableLiveData<ArrayList<BreadcrumbItem>?> = MutableLiveData()
    private val breadcrumbList = ArrayList<BreadcrumbItem>()
    private val encryptedFile: MutableLiveData<EncFile?> = MutableLiveData()
    var isSearchActive = false
    var activity : Activity? = null

    fun getPublicIndex(): LiveData<Indexing?> {
        return publicIndex
    }

    fun setPublicIndex(publicIndex: Indexing?) {
        this.publicIndex.value = publicIndex
    }

    fun getActiveRepository(): LiveData<ActiveRepository?> {
        return activeRepository
    }

    fun setActiveRepository(activeRepository: ActiveRepository?) {
        this.activeRepository.value = activeRepository
    }

    fun getDisplayItemsList(): LiveData<ArrayList<DisplayItem>?> {
        return displayItemsList
    }

    fun getBreadcrumb(): LiveData<ArrayList<BreadcrumbItem>?> {
        return breadcrumb
    }

    fun setDisplayItemsList(displayItemsList: ArrayList<DisplayItem>?) {
        this.displayItemsList.value = displayItemsList
    }

    fun appInitialisationFlow() {

        if (Utils.isLoggedIn) {
            //TODO Download or create indexing file
            /**
             * Read remote and local indexing file if they exists
             */
            val localIndexing = Utils.getIndexingToModel("loc_enc.enc")
            val remoteIndexing: Indexing? = null // Utils.getIndexingToModel("rem_enc.enc");
            var bothAreUsable = true
            var whichIndexesAreUsable = 2 //0 = local,1 = remote, 2 = both, 3 = none
            if (localIndexing == null || remoteIndexing == null) {
                bothAreUsable = false
            }
            if (!bothAreUsable) {
                whichIndexesAreUsable = if (remoteIndexing == null && localIndexing != null) 0 // local
                else if (localIndexing == null && remoteIndexing != null) 1 //remote
                else {
                    //no indexing is usable
                    3 //none
                }
            }
            when (whichIndexesAreUsable) {
                0 -> setPublicIndex(localIndexing)
                1 -> setPublicIndex(remoteIndexing)
                2 -> if (remoteIndexing!!.equals(localIndexing)) {
                    //check timestamps
                    setPublicIndex(
                            if (remoteIndexing.timestamp > localIndexing!!.timestamp) remoteIndexing else localIndexing
                    )
                } else {
                    //merge indexes into one
                    val newIndex = Indexing()
                    newIndex.repositories = localIndexing!!.mergeRepositories(remoteIndexing)
                    setPublicIndex(newIndex)
                }
                3 -> setPublicIndex(Indexing())
            }
            Log.e("Thanos", "----- fill UI -----")
            if( getActiveRepository().value != null )
                createDisplayItemsList(getActiveRepository().value?.repositories, getActiveRepository().value?.files)
            else
                createDisplayItemsList(getPublicIndex().value?.repositories, null)
        } else {
            /**
             *
             * Login || Register
             *
             */
            var intent =  Intent(activity, Login::class.java)
            activity?.startActivity(intent)

        }
    }

    fun addPassphrase(context: Context, passphrase: String?) {
        val key = Utils.getPassphraseSecretKey("passPhraseKey")
        val settings = context.getSharedPreferences(
                "EncDec_Preferences", ContextWrapper.MODE_PRIVATE)
        val editor = settings.edit()
        try {
            val encryptedPassphrase = Utils.encryptStr(passphrase!!, key)
            editor.putString("PASSPHRASE", encryptedPassphrase)
            editor.apply()
            appInitialisationFlow()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPassphrase(context: Context): String {
        val key = Utils.getPassphraseSecretKey("passPhraseKey")
        val settings = context.getSharedPreferences(
                "EncDec_Preferences", ContextWrapper.MODE_PRIVATE)
        val encryptedPassphrase = settings.getString("PASSPHRASE", "")
        try {
            return Utils.decryptStr(encryptedPassphrase!!, key)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun searchFor(searchTerm: String?) {
        val displayItems = searchFor(getPublicIndex().value!!.repositories, searchTerm)//searchFor(getPublicIndex().value, searchTerm)
        displayItemsList.value = displayItems
    }

    fun createDisplayItemsList(repositories: ArrayList<Repository>?, files: ArrayList<EncFile>?) {
        val displayItems = ArrayList<DisplayItem>()
        if (repositories != null && repositories.size > 0) {
            val executorService = Executors.newCachedThreadPool()
            val reposSearched = intArrayOf(0)
            for (repo in repositories) {
                val callbackTask = CallbackTask()
                callbackTask.task = Runnable {
                    val dsp = DisplayItem(repo.name, repo.id, "repo", repo.timestamp, activeRepository.value)
                    displayItems.add(dsp)
                    callbackTask.callback?.run()
                }
                callbackTask.callback = Runnable {
                    reposSearched[0]++
                    if (reposSearched[0] == repositories.size) {
                        executorService.shutdown()
                        displayItems.sortWith(Comparator { a: DisplayItem, b: DisplayItem ->
                            when{
                                a.timestamp < b.timestamp -> -1
                                a.timestamp > b.timestamp -> 1
                                else -> 0
                            }
                        })
                        activity?.runOnUiThread { setDisplayItemsList(displayItems) }

                    }
                }
                executorService.submit(callbackTask)
            }
            executorService.shutdown()
            try {
                executorService.awaitTermination((repositories.size * 0.5).toLong(), TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        if (files != null && files.size > 0) {
            val executorServiceForFiles = Executors.newCachedThreadPool()
            if (files != null && files.size > 0) {
                val filesSearched = intArrayOf(0)
                for (file in files) {
                    val callbackTask = CallbackTask()
                    callbackTask.task = Runnable {
                        displayItems.add(DisplayItem(file.decryptedName, file.id, "file", file.timestamp, activeRepository.value))
                        callbackTask.callback?.run()
                    }
                    callbackTask.callback = Runnable {
                        filesSearched[0]++
                        if (filesSearched[0] == files.size) {
                            executorServiceForFiles.shutdown()
                            displayItems.sortWith(Comparator { a: DisplayItem, b: DisplayItem ->
                                when{
                                    a.timestamp < b.timestamp -> -1
                                    a.timestamp > b.timestamp -> 1
                                    else -> 0
                                }
                            })
                            activity?.runOnUiThread { setDisplayItemsList(displayItems) }
                        }
                    }
                    executorServiceForFiles.submit(callbackTask)
                }
                try {
                    executorServiceForFiles.awaitTermination((files.size * 0.5).toLong(), TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        setDisplayItemsList(displayItems)
    }

    fun addRepository(repositoryName: String?) {
        val indexing = getPublicIndex().value
        val activeRepo: Repository? = getActiveRepository().value
        if (indexing?.repositories?.size == 0) {
            /**
             *
             * add repo to indexing
             *
             */
            indexing.repositories.add(Repository(Utils.createUniqueId(indexing, false), repositoryName, System.currentTimeMillis()/1000))
            createDisplayItemsList(indexing.repositories, null)
        } else {
            if (activeRepo == null) {
                /**
                 *
                 * add repo to indexing
                 *
                 */
                indexing?.repositories?.add(Repository(Utils.createUniqueId(indexing, false), repositoryName, System.currentTimeMillis()/1000))
                createDisplayItemsList(indexing?.repositories, null)
            } else {
                /**
                 *
                 * add repo to active repository
                 *
                 */

                activeRepo.repositories.add(Repository(Utils.createUniqueId(indexing!!, false), repositoryName, System.currentTimeMillis()/1000))
                createDisplayItemsList(activeRepo.repositories, activeRepo.files)
            }
        }
        setPublicIndex(indexing)
        try {
            var indexingFilePath = Utils.getContext()?.filesDir!!.absolutePath+"/"
            Utils.writeToFile( "loc_enc_temp.enc",
                    Utils.indexingToJsonStringFromModel(indexing)
            )
            Utils.encryptFile(indexingFilePath,
                    "loc_enc_temp",
                    "enc",
                    "loc_enc", true)
            Utils.clearFile(indexingFilePath + "loc_enc_temp.enc")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun chooseRepository(item: DisplayItem) {
        val activeRepo: Repository? = getActiveRepository().value
        val repoIndex: Int
        val activeRepository: ActiveRepository
        if (activeRepo == null && !isSearchActive || isSearchActive && item.parentRepository == null) {
            repoIndex = Utils.findRepoById(publicIndex.value!!.getIndxRepositories(), item.id)
            breadcrumbList.add(BreadcrumbItem(publicIndex.value!!.getIndxRepositories()[repoIndex].name!!, View.OnClickListener {
                for (i in breadcrumbList.indices.reversed()) {
                    goToRepoInBreadcrumb(publicIndex.value!!.getIndxRepositories()[repoIndex])
                }
            }))
            activeRepository = publicIndex.value!!.getIndxRepositories()[repoIndex].repoToActiveRepo()
            setActiveRepository(activeRepository)
        } else {
            repoIndex = Utils.findRepoById(item.parentRepository.repositories, item.id)
            if(repoIndex > -1) {

                activeRepository = item.parentRepository.getRepoRepositories()[repoIndex].repoToActiveRepo()
                activeRepository.parentRepository = item.parentRepository
                breadcrumbList.add(BreadcrumbItem(activeRepository.name!!, View.OnClickListener { goToRepoInBreadcrumb(activeRepo!!.getRepoRepositories()[repoIndex]) }))
                setActiveRepository(activeRepository)
            }
        }

        breadcrumb.value = breadcrumbList
        createDisplayItemsList(getActiveRepository().value?.getRepoRepositories(), getActiveRepository().value?.files)
    }

    fun chooseFile( context: Context?, v: View?, listener: PopupMenu.OnMenuItemClickListener?) {
        val popup = PopupMenu(context, v)
        popup.setOnMenuItemClickListener(listener)
        popup.inflate(R.menu.popup_menu)
        popup.show()
    }

    fun deleteFile(index: Int) {
        val indexing = getPublicIndex().value
        val activeRepo: Repository? = getActiveRepository().value
        activeRepo?.files?.removeAt(index)
        setPublicIndex(indexing)
        try {
            var indexingFilePath = Utils.getContext()?.filesDir!!.absolutePath+"/"
            Utils.writeToFile( "loc_enc_temp.enc",
                    Utils.indexingToJsonStringFromModel(indexing)
            )
            Utils.encryptFile(indexingFilePath,
                    "loc_enc_temp",
                    "enc",
                    "loc_enc", true)
            Utils.clearFile(indexingFilePath + "loc_enc_temp.enc")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        createDisplayItemsList(activeRepo?.repositories, activeRepo?.files)
    }

    fun goToRepoInBreadcrumb(repo: Repository?) {
        if (repo != null) {
            if (repo.id !== getActiveRepository().value?.id) {
                val listSize = breadcrumbList.size - 1
                for (i in listSize downTo 0) {
                    if (breadcrumbList[i].text != repo.name) {
                        breadcrumbList.removeAt(i)
                        break
                    }
                }
                setActiveRepository(repo.repoToActiveRepo())
                createDisplayItemsList(repo.repositories, repo.files)
            }
        } else {
            setActiveRepository(null)
            breadcrumbList.clear()
            createDisplayItemsList(getPublicIndex().value?.repositories, null)
        }
        breadcrumb.value = breadcrumbList
    }

    fun addFile(activity: Activity, filepath: String) {
        val activeRepo: Repository? = getActiveRepository().value
        if (activeRepo != null) {
            val srcFilePath = filepath.substring(0, filepath.lastIndexOf("/") + 1)
            val srcFileNameArr = filepath.substring(filepath.lastIndexOf("/") + 1, filepath.length).split("\\.".toRegex()).toTypedArray()
            val fl = Utils.encryptFile(srcFilePath, srcFileNameArr[0], srcFileNameArr[1], null)
            if (fl != null) {
                fl.id = Utils.createUniqueId(getPublicIndex().value!!, true)
                fl.decryptedName = srcFileNameArr[0]
                Utils.filesToReposMap[fl.id!!] = activeRepo
                activity.runOnUiThread { encryptedFile.value = fl }
            } else {
                addFile(activity, filepath)
            }
        }
    }

    val justEncryptedFile: LiveData<EncFile?>
        get() = encryptedFile

    fun saveFileToIndex(encFile: EncFile?) {
        val indexing = getPublicIndex().value

        var repo : Repository? = searchForRepoId( indexing!!.getIndxRepositories(),Utils.filesToReposMap[encFile?.id]?.id )

        if (encFile != null) {
            encFile.id = Utils.createUniqueId(indexing!!, true)
            repo?.files?.add(encFile)
            setPublicIndex(indexing)
            try {
                var indexingFilePath = Utils.getContext()?.filesDir!!.absolutePath+"/"
                Utils.writeToFile( "loc_enc_temp.enc",
                        Utils.indexingToJsonStringFromModel(indexing)
                )
                Utils.encryptFile(indexingFilePath,
                        "loc_enc_temp",
                        "enc",
                        "loc_enc", true)
                Utils.clearFile(indexingFilePath + "loc_enc_temp.enc")
                Utils.filesToReposMap.remove(encFile.id!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if(getActiveRepository().value != null && getActiveRepository().value!!.equals(repo))
                createDisplayItemsList(repo?.repositories, repo?.files)
        }
    }

    fun goBack() {
        if (getActiveRepository().value == null || getActiveRepository().value?.parentRepository == null) {
            setActiveRepository(null)
        } else {
            if (getActiveRepository().value?.parentRepository != null) {
                setActiveRepository(getActiveRepository().value?.parentRepository?.repoToActiveRepo())
            } else {
                setActiveRepository(getActiveRepository().value)
            }
        }
        if (breadcrumbList.size > 0) breadcrumbList.removeAt(breadcrumbList.size - 1)
        breadcrumb.value = breadcrumbList
    }

    private fun searchFor(repos :  ArrayList<Repository>, searchTerm: String?) :  ArrayList<DisplayItem>{
        val searchResults = ArrayList<DisplayItem>()
        if (repos != null && repos.size > 0) {
            for (repo in repos) {
                if (repo.name!!.contains(searchTerm!!)) {
                    searchResults.add(DisplayItem(repo.name, repo.id, "repo", repo.timestamp, null))
                }
                if(repo.repositories.isNotEmpty())
                    searchResults.addAll(searchFor(repo.repositories, searchTerm))
            }
        }
        return searchResults
    }

    private fun searchForRepoId(repos :  ArrayList<Repository>, repoId: String?) : Repository {
        var searchResults = ArrayList<Repository>()
        if (repos.size > 0) {
            searchResults = getAllRepos(repos)
            for (result in getAllRepos(repos)) {
                if (!result.id.equals(repoId))
                    searchResults.remove(result)
            }
        }

        return searchResults.first()
    }

    fun getAllRepos(repos : ArrayList<Repository>) : ArrayList<Repository>{
        val searchResults = ArrayList<Repository>()

        for(repo in repos){
            searchResults.addAll(repo.getRepoRepositories())
            if(repo.repositories.isNotEmpty())
                searchResults.addAll(getAllRepos(repo.getRepoRepositories()))
        }
        return searchResults
    }

     fun returnFullIndexPath(repos :  ArrayList<Repository>, repoId: String?) : ArrayList<Int>{
        var indexPath = ArrayList<Int>()

         repos.sortWith(Comparator { a: Repository, b: Repository ->
            when{
                a.timestamp < b.timestamp -> -1
                a.timestamp > b.timestamp -> 1
                else -> 0
            }
        })

        if (repos != null && repos.size > 0) {
            for ((indx, repo) in repos.withIndex()) {
                if (repo.id.equals(repoId)) {
                    indexPath.add(indx)
                } else if(repo.repositories.isNotEmpty()) {
                        if(repo.repositories.size > indx) indexPath.add(indx)
                        indexPath.addAll(returnFullIndexPath(repo.repositories, repoId))
                } else {
                    if(indexPath.size > 0)
                        indexPath.removeAt(indexPath.size-1)
                }
            }
        }
        return indexPath
    }

    companion object {



        fun searchFor(indexing: Indexing?, searchTerm: String?): ArrayList<DisplayItem> {
            val executorService = Executors.newCachedThreadPool()
            val searchResults = ArrayList<DisplayItem>()
            val repos = indexing?.repositories
            if (repos != null && repos.size > 0) {
                val reposSearched = intArrayOf(0)
                for (repo in repos) {
                    val callbackTask = CallbackTask()
                    callbackTask.task = Runnable {
                        if (repo.name!!.contains(searchTerm!!)) {
                            searchResults.add(DisplayItem(repo.name, repo.id, "repo", repo.timestamp, null))
                        }
                        searchResults.addAll(repo.searchForFiles(searchTerm))
                        searchResults.addAll(repo.searchForRepos(searchTerm))
                        callbackTask.callback?.run()
                    }
                    callbackTask.callback = Runnable {
                        reposSearched[0]++
                        if (reposSearched[0] == repos.size) {
                            executorService.shutdown()
                            searchResults.sortWith(Comparator { a: DisplayItem, b: DisplayItem ->
                                when{
                                    a.timestamp < b.timestamp -> -1
                                    a.timestamp > b.timestamp -> 1
                                    else -> 0
                                }
                            })
                        }
                    }
                    executorService.submit(callbackTask)
                }
            }
            return searchResults
        }
    }
}