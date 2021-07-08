package com.athanasioua.battleship.model.newp.model

import android.util.Log
import com.athanasioua.battleship.model.newp.Utils
import java.util.*


open class Repository {
    var id: String? = null
    var name: String? = null
    var files = ArrayList<EncFile>()
    var timestamp: Long = 0
    var repositories = ArrayList<Repository>()

    constructor() {}
    constructor(id: String?, name: String?, timestamp : Long) {
        this.id = id
        this.name = name
        this.timestamp = timestamp
    }

    override fun equals(o: Any?): Boolean {
        // self check
        if (this === o) return true
        // null check
        if (o == null) return false
      /*  // type check and cast
        if (javaClass != o.javaClass) return false*/
        val repository = o as Repository

        return id == repository.id &&
                name == repository.name &&
                timestamp == repository.timestamp &&
                compareRepositories(repository) &&
                compareFiles(repository)
    }

    private fun compareFiles(repositoryToCompare: Repository): Boolean {
        if (files.size != repositoryToCompare.files.size) return false
        var areEqual = true
        val fileList = files
        val fileListToCompare = repositoryToCompare.files
        Collections.sort(fileList, Comparator { a: EncFile, b: EncFile -> a.id!!.compareTo(b.id!!) } as Comparator<EncFile>)
        Collections.sort(fileListToCompare, Comparator { a: EncFile, b: EncFile -> a.id!!.compareTo(b.id!!) } as Comparator<EncFile>)
        for (i in fileList.indices) {
            if (!fileList[i].equals(fileListToCompare[i])) {
                areEqual = false
            }
        }
        return areEqual
    }

    fun mergeRepoFiles(repo2: Repository): ArrayList<EncFile> {
        val fileArrayList = ArrayList(files)
        val filesToAdd = ArrayList<EncFile>()
        var fileIndex: Int
        for (repo2File in repo2.files) {
            fileIndex = findFileById(fileArrayList, repo2File.id)
            if (fileIndex == -1) {
                filesToAdd.add(repo2File)
            } else {
                val existingFile = fileArrayList[fileIndex]
                //compare timestamps
                if (existingFile.timestamp < repo2File.timestamp) {
                    fileArrayList[fileIndex] = repo2File
                }
            }
        }
        fileArrayList.addAll(filesToAdd)
        return fileArrayList
    }

    fun findFileById(filesList: ArrayList<EncFile>, id: String?): Int {
        for (i in filesList.indices) {
            if (filesList[i].id == id) {
                return i
            }
        }
        return -1
    }

    fun searchForFiles(searchTerm: String?): ArrayList<DisplayItem> {
        val searchResults = ArrayList<DisplayItem>()
        for (file in files) {
            if (file.decryptedName != null && file.decryptedName!!.contains(searchTerm!!)) {
                searchResults.add(DisplayItem(file.decryptedName!!, file.id!!, "file", file.timestamp, this))
            }
        }
        return searchResults
    }

    private fun compareRepositories(repositoryToCompare: Repository): Boolean {
        if (repositories.size != repositoryToCompare.repositories.size) return false
        var areEqual = true
        val repoList = repositories
        val repoListToCompare = repositoryToCompare.repositories
        Collections.sort(repoList, Comparator { a: Repository, b: Repository -> a.id!!.compareTo(b.id!!) } as Comparator<Repository>)
        Collections.sort(repoListToCompare, Comparator { a: Repository, b: Repository -> a.id!!.compareTo(b.id!!) } as Comparator<Repository>)
        for (i in repoList.indices) {
            if (repoList[i] != repoListToCompare[i]) {
                areEqual = false
            }
        }
        return areEqual
    }

    fun mergeRepositories(repo2: Repository): ArrayList<Repository> {
        val repositoryArrayList = ArrayList(repositories)
        val reposToAdd = ArrayList<Repository>()
        var repoIndex: Int
        for (repo2Repo in repo2.repositories) {
            repoIndex = Utils.findRepoById(repositoryArrayList, repo2Repo.id!!)
            if (repoIndex == -1) {
                reposToAdd.add(repo2Repo)
            } else {
                val existingRepo = repositoryArrayList[repoIndex]
                existingRepo.files = existingRepo.mergeRepoFiles(repo2Repo)
                //compare timestamps
                if (existingRepo.timestamp < repo2Repo.timestamp) {
                    existingRepo.name = repo2Repo.name
                    existingRepo.timestamp = Date().time
                }
                repositoryArrayList[repoIndex] = existingRepo
            }
        }
        repositoryArrayList.addAll(reposToAdd)
        return repositoryArrayList
    }

    fun searchForRepos(searchTerm: String?): ArrayList<DisplayItem> {
        val searchResults = ArrayList<DisplayItem>()
        for (repository in repositories) {
            if (repository.name != null && repository.name!!.contains(searchTerm!!)) {
                searchResults.add(DisplayItem(repository.name!!, repository.id!!, "repo", repository.timestamp, this))
            }
        }
        return searchResults
    }

    fun repoToActiveRepo(): ActiveRepository {
        var ac = ActiveRepository()
        ac.id = id
        ac.name = name
        ac.files = files
        ac.repositories = repositories
        ac.timestamp = timestamp
        return ac
    }

    fun getRepoRepositories(): ArrayList<Repository> {
        repositories.sortWith(Comparator { a: Repository, b: Repository ->
            when{
                a.timestamp < b.timestamp -> -1
                a.timestamp > b.timestamp -> 1
                else -> 0
            }
        })
        return repositories
    }
}
