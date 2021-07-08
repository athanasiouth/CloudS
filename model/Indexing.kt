package com.athanasioua.battleship.model.newp.model

import com.athanasioua.battleship.model.newp.Utils
import java.util.*


class Indexing {
    var timestamp: Long = 0
    var repositories = ArrayList<Repository>()


    override fun equals(o: Any?): Boolean {
        // self check
        if (this === o) return true
        // null check
        if (o == null) return false
        // type check and cast
        if (javaClass != o.javaClass) return false
        val indexing = o as Indexing
        return compareRepositories(indexing)
    }

    private fun compareRepositories(indexingToCompare: Indexing): Boolean {
        if (repositories.size != indexingToCompare.repositories.size) return false
        var areEqual = true
        val repoList = repositories
        val repoListToCompare = indexingToCompare.repositories
        Collections.sort(repoList, Comparator { a: Repository, b: Repository -> a.id!!.compareTo(b.id!!) } as Comparator<Repository>)
        Collections.sort(repoListToCompare, Comparator { a: Repository, b: Repository -> a.id!!.compareTo(b.id!!) } as Comparator<Repository>)
        for (i in repoList.indices) {
            if (!repoList[i].equals(repoListToCompare[i])) {
                areEqual = false
            }
        }
        return areEqual
    }

    fun mergeRepositories(index2: Indexing): ArrayList<Repository> {
        val repositoryArrayList = ArrayList(repositories)
        val reposToAdd = ArrayList<Repository>()
        var repoIndex: Int
        for (index2Repo in index2.repositories) {
            repoIndex = Utils.findRepoById(repositoryArrayList, index2Repo.id!!)
            if (repoIndex == -1) {
                reposToAdd.add(index2Repo)
            } else {
                val existingRepo = repositoryArrayList[repoIndex]
                existingRepo.files = existingRepo.mergeRepoFiles(index2Repo)
                existingRepo.repositories = existingRepo.mergeRepositories(index2Repo)
                //compare timestamps
                if (existingRepo.timestamp < index2Repo.timestamp) {
                    existingRepo.name = index2Repo.name
                    existingRepo.timestamp = Date().time
                }
                repositoryArrayList[repoIndex] = existingRepo
            }
        }
        repositoryArrayList.addAll(reposToAdd)
        return repositoryArrayList
    }

    fun getIndxRepositories(): ArrayList<Repository> {
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
