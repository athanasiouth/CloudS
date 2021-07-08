package com.athanasioua.battleship.model.newp.model;

import java.util.Objects;

public class EncFile_old {
    private String id;
    private String name;
    private String decryptedName;
    private String lastNameUsed;
    private String extension;
    private long timestamp;

    public EncFile_old(String name, String extension){
        this.name = name;
        this.extension = extension;
    }

    public EncFile_old(String id, String name, String extension){
        this.id = id;
        this.name = name;
        this.extension = extension;
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

    public String getDecryptedName() { return decryptedName; }

    public void setDecryptedName(String decryptedName) { this.decryptedName = decryptedName;   }

    public String getLastNameUsed() {
        return lastNameUsed;
    }

    public void setLastNameUsed(String lastNameUsed) {
        this.lastNameUsed = lastNameUsed;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
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
        EncFile file = (EncFile) o;
        return  Objects.equals(getId(), file.getId()) &&
                Objects.equals(getName(), file.getName()) &&
                Objects.equals(getLastNameUsed(), file.getLastNameUsed()) &&
                Objects.equals(getExtension(), file.getExtension()) &&
                Objects.equals(getTimestamp(), file.getTimestamp());
    }

}
