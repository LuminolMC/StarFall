package moe.luminolmc.data

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.bson.Document

@Serializable
data class DownloadInfo  (
    @SerializedName("release_tag")
    private val releaseTag: String,
    @SerializedName("branch_name")
    private val branch: String,
    @SerializedName("mc_version")
    private val mcVersion: String,
    @SerializedName("commits")
    private val commits: List<CommitData>,
    @SerializedName("download_link")
    private val downloadLink: String
): MongodbSerializableData {
    override fun toDocument(): Document = Document.parse(gson.toJson(this))

    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

        fun fromDocument(document: Document): DownloadInfo = gson.fromJson(document.toJson(), DownloadInfo::class.java)
    }
}

class DownloadInfoDatabaseService(
    private val database: MongoDatabase
) {
    private val downloadInfos: MongoCollection<Document>

    init {
        this.database.createCollection("downloads")
        this.downloadInfos = database.getCollection("downloads")
    }

    suspend fun getDownloadInfo(tag: String): DownloadInfo? = withContext(Dispatchers.IO) {
        return@withContext downloadInfos.find(Filters.eq("release_tag", tag)).map { DownloadInfo.fromDocument(it) }.first()
    }

    suspend fun add(downloadInfo: DownloadInfo) = withContext(Dispatchers.IO) {
        val doc = downloadInfo.toDocument()
        downloadInfos.insertOne(doc)
    }

    suspend fun getAllOf(branch: String): List<DownloadInfo> = withContext(Dispatchers.IO) {
        return@withContext downloadInfos.find(Filters.eq("branch_name", branch)).map { DownloadInfo.fromDocument(it) }.toList()
    }
}