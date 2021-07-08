package com.athanasioua.battleship.model.newp.model;

import com.athanasioua.battleship.model.newp.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class Indexing_old {

    private long timestamp;
    private ArrayList<Repository> repositories = new ArrayList<>();

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<Repository> getRepositories() {
        return repositories;
    }

    public void setRepositories(ArrayList<Repository> repositories) {
        this.repositories = repositories;
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;
        // null check
        if (o == null)
            return false;
        // type check and cast
        if (getClass() != o.getClass())
            return false;
        Indexing indexing = (Indexing) o;
        return compareRepositories(indexing);
    }

    private boolean  compareRepositories(Indexing indexingToCompare){
        if ( getRepositories().size() != indexingToCompare.getRepositories().size() )
            return false;
        boolean areEqual = true;

        ArrayList<Repository> repoList = getRepositories();
        ArrayList<Repository> repoListToCompare = indexingToCompare.getRepositories();
        Collections.sort(repoList, (Comparator<Repository>) (a, b) -> a.getId().compareTo( b.getId()));
        Collections.sort(repoListToCompare, (Comparator<Repository>) (a, b) -> a.getId().compareTo( b.getId()));

        for(int i = 0;i < repoList.size();i++){
            if (!repoList.get(i).equals(repoListToCompare.get(i))) {
                areEqual = false;
            }
        }
        return areEqual;
    }

    public ArrayList<Repository> mergeRepositories(Indexing index2){
        ArrayList<Repository> repositoryArrayList = new ArrayList<>(getRepositories());
        ArrayList<Repository> reposToAdd = new ArrayList<>();
        int repoIndex;
        for(Repository index2Repo : index2.getRepositories()){
            repoIndex = Utils.findRepoById(repositoryArrayList,index2Repo.getId());
            if( repoIndex == -1) {
                reposToAdd.add(index2Repo);
            }else{
                Repository existingRepo = repositoryArrayList.get(repoIndex);
                existingRepo.setFiles(existingRepo.mergeRepoFiles(index2Repo));
                existingRepo.setRepositories(existingRepo.mergeRepositories(index2Repo));
                //compare timestamps
                if(existingRepo.getTimestamp() < index2Repo.getTimestamp()){
                    existingRepo.setName(index2Repo.getName());
                    existingRepo.setTimestamp(new Date().getTime());
                }
                repositoryArrayList.set(repoIndex,existingRepo);
            }
        }
        repositoryArrayList.addAll(reposToAdd);
        return repositoryArrayList;
    }

}
