package com.athanasioua.battleship.model.newp.model;

import android.util.Log;

import com.athanasioua.battleship.model.newp.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

public class Repository_old {
    public String id;
    public String name;
    public ArrayList<EncFile> files  = new ArrayList<>();
    public long timestamp;
    public ArrayList<Repository> repositories = new ArrayList<>();

    public Repository_old() {
    }

    public Repository_old(String id, String name) {
        this.id = id;
        this.name = name;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<EncFile> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<EncFile> files) {
        this.files = files;
    }

    public ArrayList<Repository> getRepositories() {
        return repositories;
    }

    public void setRepositories(ArrayList<Repository> repositories) {
        this.repositories = repositories;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
        Repository repository = (Repository) o;
        Log.e("Thanos 000000000000", String.valueOf(repository.getName()));
        Log.e("Thanos 1", String.valueOf(Objects.equals(getId(), repository.getId())));
        Log.e("Thanos 2", String.valueOf(Objects.equals(getName(), repository.getName())));
        Log.e("Thanos 3", String.valueOf(compareFiles(repository)));

        return  Objects.equals(getId(), repository.getId()) &&
                Objects.equals(getName(), repository.getName()) &&
                Objects.equals(getTimestamp(), repository.getTimestamp()) &&
                compareRepositories(repository) &&
                compareFiles(repository);
    }

    private boolean compareFiles(Repository repositoryToCompare){
        if ( getFiles().size() != repositoryToCompare.getFiles().size() )
            return false;
        boolean areEqual = true;

        ArrayList<EncFile> fileList = getFiles();
        ArrayList<EncFile> fileListToCompare = repositoryToCompare.getFiles();
        Collections.sort(fileList, (Comparator<EncFile>) (a, b) -> a.getId().compareTo( b.getId()));
        Collections.sort(fileListToCompare, (Comparator<EncFile>) (a, b) -> a.getId().compareTo( b.getId()));

        for(int i = 0;i < fileList.size();i++){
            if (!fileList.get(i).equals(fileListToCompare.get(i))) {
                areEqual = false;
            }
        }

        return areEqual;
    }

    public ArrayList<EncFile> mergeRepoFiles(Repository repo2){
        ArrayList<EncFile> fileArrayList = new ArrayList<>(getFiles());
        ArrayList<EncFile> filesToAdd = new ArrayList<>();
        int fileIndex;
        for(EncFile repo2File : repo2.getFiles()){
            fileIndex = findFileById(fileArrayList,repo2File.getId());
            if( fileIndex == -1) {
                filesToAdd.add(repo2File);
            }else{
                EncFile existingFile = fileArrayList.get(fileIndex);
                //compare timestamps
                if(existingFile.getTimestamp() < repo2File.getTimestamp()){
                    fileArrayList.set(fileIndex,repo2File);
                }
            }
        }
        fileArrayList.addAll(filesToAdd);
        return fileArrayList;
    }

    public int findFileById(ArrayList<EncFile> filesList, String id){
        for(int i = 0; i < filesList.size(); i++){
            if(filesList.get(i).getId().equals(id)){
                return i;
            }
        }
        return -1;
    }

   /* public ArrayList<DisplayItem> searchForFiles(String searchTerm){
        ArrayList<DisplayItem> searchResults = new ArrayList<>();

        for(EncFile file : getFiles()){
            if(file.getDecryptedName() != null && file.getDecryptedName().contains(searchTerm)){
                searchResults.add(new DisplayItem(file.getDecryptedName(), file.getId(),"file", file.getTimestamp(),this));
            }
        }
        return searchResults;
    }*/

    private boolean  compareRepositories(Repository repositoryToCompare){
        if ( getRepositories().size() != repositoryToCompare.getRepositories().size() )
            return false;
        boolean areEqual = true;

        ArrayList<Repository> repoList = getRepositories();
        ArrayList<Repository> repoListToCompare = repositoryToCompare.getRepositories();
        Collections.sort(repoList, (Comparator<Repository>) (a, b) -> a.getId().compareTo( b.getId()));
        Collections.sort(repoListToCompare, (Comparator<Repository>) (a, b) -> a.getId().compareTo( b.getId()));

        for(int i = 0;i < repoList.size();i++){
            if (!repoList.get(i).equals(repoListToCompare.get(i))) {
                areEqual = false;
            }
        }
        return areEqual;
    }

    public ArrayList<Repository> mergeRepositories(Repository repo2){
        ArrayList<Repository> repositoryArrayList = new ArrayList<>(getRepositories());
        ArrayList<Repository> reposToAdd = new ArrayList<>();
        int repoIndex;
        for(Repository repo2Repo : repo2.getRepositories()){
            repoIndex = Utils.findRepoById(repositoryArrayList,repo2Repo.getId());
            if( repoIndex == -1) {
                reposToAdd.add(repo2Repo);
            }else{
                Repository existingRepo = repositoryArrayList.get(repoIndex);
                existingRepo.setFiles(existingRepo.mergeRepoFiles(repo2Repo));
                //compare timestamps
                if(existingRepo.getTimestamp() < repo2Repo.getTimestamp()){
                    existingRepo.setName(repo2Repo.getName());
                    existingRepo.setTimestamp(new Date().getTime());
                }
                repositoryArrayList.set(repoIndex,existingRepo);
            }
        }
        repositoryArrayList.addAll(reposToAdd);
        return repositoryArrayList;
    }


    /*public ArrayList<DisplayItem> searchForRepos(String searchTerm){
        ArrayList<DisplayItem> searchResults = new ArrayList<>();

        for(Repository repository : getRepositories()){
            if(repository.getName() != null && repository.getName().contains(searchTerm)){
                searchResults.add(new DisplayItem(repository.getName(), repository.getId(),"repo", repository.getTimestamp(),this));
            }
        }
        return searchResults;
    }*/

    public ActiveRepository repoToActiveRepo(){
        ActiveRepository ac = new ActiveRepository();
        ac.setId(this.getId());
        ac.setName(this.getName());
        ac.setFiles(this.getFiles());
        ac.setRepositories(this.getRepositories());

        return ac;
    }
}
