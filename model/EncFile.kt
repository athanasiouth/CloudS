package com.athanasioua.battleship.model.newp.model

class EncFile {
    var id: String? = null
    var name: String
    var decryptedName: String? = null
    var lastNameUsed: String? = null
    var extension: String
    var timestamp: Long = 0

    constructor(name: String, extension: String, timestamp : Long) {
        this.name = name
        this.extension = extension
        this.timestamp = timestamp
    }

    constructor(id: String?, name: String, extension: String) {
        this.id = id
        this.name = name
        this.extension = extension
    }

    override fun equals(o: Any?): Boolean {
        // self check
        if (this === o) return true
        // null check
        if (o == null) return false
        // type check and cast
        if (javaClass != o.javaClass) return false
        val file = o as EncFile
        return id == file.id &&
                name == file.name &&
                lastNameUsed == file.lastNameUsed &&
                extension == file.extension &&
                timestamp == file.timestamp
    }
}