package com.athanasioua.battleship.model.newp

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Context.MODE_WORLD_READABLE
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import com.athanasioua.battleship.model.newp.model.DisplayItem
import com.athanasioua.battleship.model.newp.model.EncFile
import com.athanasioua.battleship.model.newp.model.Indexing
import com.athanasioua.battleship.model.newp.model.Repository
import com.athanasioua.battleship.model.newp.viewmodel.EncDec
import com.google.gson.Gson
import java.io.*
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.security.*
import java.security.cert.CertificateException
import java.security.spec.InvalidKeySpecException
import java.security.spec.InvalidParameterSpecException
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Utils {
    @JvmField
    var encryptedFilePath = Environment.getExternalStorageDirectory().toString() + "/Pictures/"
    private val decryptedFilePath = Environment.getExternalStorageDirectory().toString() + "/Pictures/decrypted/"
    private const val indexingFileName = "test_enc.enc"
    private var password = "123456"
    private const val algorithm = "AES/GCM/NoPadding"
    private var fileToDecript: ByteArray? = null
    private var keyStore: KeyStore? = null
    private var context: Context? = null
    var filesToReposMap = HashMap<String, Repository>()
    var isLoggedIn : Boolean = false
    var fileSelection : Boolean = false


    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class, KeyStoreException::class, IOException::class, CertificateException::class)
    fun generateKey(): SecretKey {
        val iterationCount = 1000
        val saltLength = 32 // bytes; should be the same size as the output (256 / 8 = 32)
        val keyLength = 256 // 256-bits for AES-256, 128-bits for AES-128, etc
        val salt: ByteArray // Should be of saltLength

        /* When first creating the key, obtain a salt with this: */
        val random = SecureRandom()
        salt = ByteArray(saltLength)
        random.nextBytes(salt)

        /* Use this to derive the key from the password: */
        val encDec = EncDec()
        password = encDec.getPassphrase(context!!)
        val keySpec: KeySpec = PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength)
        val keyFactory = SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA1")
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    //TODO Change alias on the key
    @get:Throws(KeyStoreException::class, CertificateException::class, NoSuchAlgorithmException::class, IOException::class, InvalidKeySpecException::class, UnrecoverableKeyException::class)
    private val yourKey: SecretKey
        private get() {
            var key: SecretKey? = null
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore?.load(null, null)
            if (keyStore!!.containsAlias("Test")) {
                //TODO Change alias on the key
                key = keyStore?.getKey("Test", null) as SecretKey
            } else {
                buildKey()
            }
            return key ?: buildKey()!!
        }

    @Throws(InvalidKeySpecException::class, CertificateException::class, NoSuchAlgorithmException::class, KeyStoreException::class, IOException::class)
    private fun buildKey(): SecretKey? {
        var key: SecretKey? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            key = generateKey()
            //TODO Change alias on the key
            keyStore?.setEntry("Test", KeyStore.SecretKeyEntry(key),
                    KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(false)
                            .build()
            )
        }
        return key
    }

    fun encryptFile(originalFilePath: String, originalFileName: String, originalFileExtension: String, fileNameToSave: String?, isIndexing : Boolean = false): EncFile? {
        var encFile: EncFile? = null
        try {
            val fis = FileInputStream("$originalFilePath$originalFileName.$originalFileExtension")
            val aes = Cipher.getInstance(algorithm)
            val secureRandom = SecureRandom()
            val iv = ByteArray(12)
            secureRandom.nextBytes(iv)
            val parameterSpec = GCMParameterSpec(128, iv)
            aes.init(Cipher.ENCRYPT_MODE, yourKey, parameterSpec)
            val fs: FileOutputStream?
            val newFile: File
            var newFilename = ByteArray(0)
            if (fileNameToSave == null || fileNameToSave == "") {
                newFilename = encryptString(originalFileName.toByteArray(StandardCharsets.UTF_8))
                fileToDecript = newFilename
                newFile = File(encryptedFilePath + String(newFilename) + ".enc")
            } else {
                /**
                 * save indexing file
                 */
                newFile = File("$encryptedFilePath$fileNameToSave.enc")
            }
            //            newFile.createNewFile();
            if(isIndexing)
                fs = context?.openFileOutput("$fileNameToSave.enc",MODE_PRIVATE);//FileOutputStream(newFile)
            else
                fs = FileOutputStream(newFile)
            fs?.write(iv)
            /** Write IV to file so it can be used in decryption  */
            val out = CipherOutputStream(fs, aes)
            var b: Int
            val d = ByteArray(12)
            while (fis.read(d).also { b = it } != -1) {
                out.write(d, 0, b)
            }
            out.flush()
            out.close()
            if (fileNameToSave == null || fileNameToSave == "") {
                /**
                 *
                 * Write file to indexing
                 *
                 */
                encFile = EncFile(Base64.encodeToString(newFilename, Base64.URL_SAFE), originalFileExtension, System.currentTimeMillis()/1000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return encFile
    }

    @JvmStatic
    fun decryptFile(encryptedFile: EncFile, isTemp: Boolean) {
        try {
            var success = true
            val outputFileName: String
            val inputFileName = encryptedFilePath + String(Base64.decode(encryptedFile.name, Base64.URL_SAFE)) + ".enc"
            if (isTemp) {
                outputFileName = context?.cacheDir.toString() + "/" + String(decryptString(Base64.decode(encryptedFile.name, Base64.URL_SAFE))) + "." + encryptedFile.extension
                val file = File(context?.cacheDir, String(decryptString(Base64.decode(encryptedFile.name, Base64.URL_SAFE))) + "." + encryptedFile.extension)
                if (!file.exists()) {
                    success = true
                }
            } else {
                outputFileName = decryptedFilePath + String(decryptString(Base64.decode(encryptedFile.name, Base64.URL_SAFE))) + "." + encryptedFile.extension
                val folder = File(decryptedFilePath)
                if (!folder.exists()) {
                    success = folder.mkdirs()
                }
            }
            if (success) {
                decryptFile(inputFileName, outputFileName)
            } else {
                //TODO SHOW ERROR MESSAGE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun decryptFile(inputFileName: String?, outputFileName: String?) {
        val fis: FileInputStream
        try {
            fis = FileInputStream(inputFileName)
            val fos = FileOutputStream(outputFileName)

            /** Read IV from file   */
            val iv = ByteArray(12)
            fis.read(iv)
            val parameterSpec = GCMParameterSpec(128, iv)
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.DECRYPT_MODE, yourKey, parameterSpec)
            val cis = CipherInputStream(fis, cipher)
            var b: Int
            val d = ByteArray(12)
            while (cis.read(d).also { b = it } != -1) {
                fos.write(d, 0, b)
            }
            fos.flush()
            fos.close()
            cis.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    fun encryptString(clear: ByteArray?): ByteArray {
        val cipher = Cipher.getInstance(algorithm)
        val secureRandom = SecureRandom()
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)
        val parameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, yourKey, parameterSpec)
        val encrypted = cipher.doFinal(clear)
        val outputStream = ByteArrayOutputStream()
        outputStream.write(iv)
        outputStream.write(encrypted)
        return outputStream.toByteArray()
    }

    @JvmStatic
    @Throws(Exception::class)
    fun decryptString(encrypted: ByteArray): ByteArray {
        if(encrypted.isNotEmpty()) {
            val iv = encrypted.copyOfRange(0, 12)
            val encr = encrypted.copyOfRange(12, encrypted.size)
            val parameterSpec = GCMParameterSpec(128, iv)
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.DECRYPT_MODE, yourKey, parameterSpec)
            return cipher.doFinal(encr)
        } else
            return "".toByteArray()
    }

    private fun decryptIndexing(indexingFileName: String): String {
        var contents = ""
        var indexingFilePath = context?.filesDir!!.absolutePath+"/"
        /**
         *
         * Path where the indexing file will be downloaded
         *
         */
        val indexingFile = File(indexingFilePath + indexingFileName)
        if (indexingFile.exists()) {
            /**
             *
             * Decrypt file - Read Contents of file
             *
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    contents = String(decryptString(Files.readAllBytes(Paths.get(indexingFilePath + indexingFileName))))
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            /**
             *
             * File does not exist - Re-download indexing file or if not exists on server create it
             *
             */
            try {
                indexingFile.createNewFile();
            } catch ( e : Exception) {
                e.printStackTrace();
            }

        }
        return contents
    }

    fun getIndexingToModel(indexingFileName: String): Indexing? {
        val json = decryptIndexing(indexingFileName)
        val indx = Gson().fromJson(json, Indexing::class.java)
        if (indx != null && indx.repositories.size > 0) {
            for (repository in indx.repositories) {
                if (repository.files.size > 0) {
                    for (encFile in repository.files) {
                        try {
                            encFile.decryptedName = String(decryptString(Base64.decode(encFile.name, Base64.URL_SAFE)))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        return indx
    }

    fun indexingToJsonStringFromModel(indexing: Indexing?): String {
        val gson = Gson()
        return gson.toJson(indexing)
    }

    fun createUniqueId(indexing: Indexing, isForFile: Boolean): String {
        val id = Base64.encodeToString(UUID.randomUUID().toString().toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE).substring(0, 20) +
                Base64.encodeToString(Date().time.toString().toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE)
        val repos = indexing.repositories
        if (isForFile) {
            /**
             *
             * At least one repository should exist and a check would be in place before the creation of the file
             *
             */
            for (repo in repos) {
                if (repo.findFileById(repo.files, id) == -1) {
                    return id
                }
            }
            createUniqueId(indexing, true)
        } else {
            if (repos.size > 0) {
                if (findRepoById(indexing.repositories, id) == -1) {
                    return id
                } else {
                    createUniqueId(indexing, false)
                }
            } else {
                return id
            }
        }
        return id
    }

    fun writeToFile(path: String, data: String?) {
        //val file = File(path)
        try {
           // file.createNewFile()
            val fOut = context?.openFileOutput(path,MODE_PRIVATE);//FileOutputStream(file)
           /* val myOutWriter = OutputStreamWriter(fOut)
            myOutWriter.append(data)
            myOutWriter.close()*/
            fOut?.write(data?.toByteArray());
            fOut?.flush()
            fOut?.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    @Throws(IOException::class)
    fun clearFile(path: String?) {
        val fwOb = FileWriter(path, false)
        val pwOb = PrintWriter(fwOb, false)
        pwOb.flush()
        pwOb.close()
        fwOb.close()
    }

    @JvmStatic
    fun findRepoById(repoList: ArrayList<Repository>, id: String): Int {
        repoList.sortWith(Comparator { a: Repository, b: Repository ->
            when{
                a.timestamp < b.timestamp -> -1
                a.timestamp > b.timestamp -> 1
                else -> 0
            }
        })
        for (i in repoList.indices) {
            if (repoList[i].id == id) {
                return i
            }
        }
        return -1
    }

    @JvmStatic
    @SuppressLint("NewApi")
    @Throws(URISyntaxException::class)
    fun getFilePath(context: Context, flUri: Uri): String? {
        var uri = flUri
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(context.applicationContext, flUri)) {
            if (isExternalStorageDocument(flUri)) {
                val docId = DocumentsContract.getDocumentId(flUri)
                val split = docId.split(":".toRegex()).toTypedArray()
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            } else if (isDownloadsDocument(flUri)) {
                val id = DocumentsContract.getDocumentId(flUri)
                if (!TextUtils.isEmpty(id)) {
                    return if (id.startsWith("raw:")) {
                        id.replaceFirst("raw:".toRegex(), "")
                    } else try {
                        val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                        getDataColumn(context, contentUri, null, null)
                    } catch (e: NumberFormatException) {
                        Log.e("FileUtils", "Downloads provider returned unexpected uri $flUri", e)
                        null
                    }
                }
            } else if (isMediaDocument(flUri)) {
                val docId = DocumentsContract.getDocumentId(flUri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                if ("image" == type) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                selection = "_id=?"
                selectionArgs = arrayOf(
                        split[1]
                )
            }
        }
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            if (isGooglePhotosUri(uri)) {
                return uri.lastPathSegment
            }
            val projection = arrayOf(
                    MediaStore.Images.Media.DATA
            )
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver
                        .query(uri, projection, selection, selectionArgs, null)
                val column_index = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor!!.moveToFirst()) {
                    return cursor.getString(column_index!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    @JvmStatic
    fun setContext(context: Context?) {
        Utils.context = context
    }

    @JvmStatic
    fun getContext() : Context? {
        return context
    }

    fun writeFile(`in`: InputStream, file: File?) {
        var out: OutputStream? = null
        try {
            out = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
                `in`.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                      selectionArgs: Array<String?>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
                column
        )
        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs,
                    null)
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     *
     * PASSPHRASE RELATED ENCRYPTION
     *
     */
    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Throws(InvalidAlgorithmParameterException::class, NoSuchProviderException::class, NoSuchAlgorithmException::class)
    fun createPassphraseKey(keyName: String?) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyName!!,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    @JvmStatic
    fun getPassphraseSecretKey(keyName: String?): SecretKey? {
        var keyStore: KeyStore? = null
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            // Before the keystore can be accessed, it must be loaded.
            keyStore.load(null)
            return keyStore.getKey(keyName, null) as SecretKey
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidParameterSpecException::class, IllegalBlockSizeException::class, BadPaddingException::class, UnsupportedEncodingException::class)
    fun encryptStr(toEncrypt: String, secret: SecretKey?): String {
        /* Encrypt the message. */
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secret)
        val iv = Base64.encodeToString(cipher.iv, Base64.DEFAULT)
        val encrypted = Base64.encodeToString(cipher.doFinal(toEncrypt.toByteArray(StandardCharsets.UTF_8)), Base64.DEFAULT)
        return "$encrypted,$iv"
    }

    @Throws(NoSuchPaddingException::class, NoSuchAlgorithmException::class, InvalidParameterSpecException::class, InvalidAlgorithmParameterException::class, InvalidKeyException::class, BadPaddingException::class, IllegalBlockSizeException::class, UnsupportedEncodingException::class)
    fun decryptStr(cipherText: String, secret: SecretKey?): String {
        val parts = cipherText.split(",".toRegex()).toTypedArray()
        if (parts.size != 2) throw AssertionError("String to decrypt must be of the form: 'BASE64_DATA" + "," + "BASE64_IV'")
        val encrypted = Base64.decode(parts[0], Base64.DEFAULT)
        val iv = Base64.decode(parts[1], Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val spec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, secret, spec)
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun getIsLoggedIn() : Boolean{
        return isLoggedIn
    }

    @JvmStatic
    fun getIsFileSelection() : Boolean{
        return fileSelection
    }

    @JvmStatic
    fun setIsFileSelection(isFileSelection : Boolean) {
        fileSelection = isFileSelection
    }
}