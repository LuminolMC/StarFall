package moe.luminolmc.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.bson.Document
import org.bson.types.ObjectId

@Serializable
data class CommitData (
    @SerializedName("commit_message")
    val message: String,
    @SerializedName("authors")
    val authors: List<String>,
    @SerializedName("commit_hash")
    val commitHash: String,
    @SerializedName("timestamp")
    val commitTimestamp: Long,
    @SerializedName("branch_name")
    val branchName: String
): MongodbSerializableData {
    override fun toDocument(): Document = Document.parse(gson.toJson(this))

    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

        fun fromJsonObject(obj: JsonObject): CommitData = gson.fromJson(obj, CommitData::class.java)

        fun fromDocument(document: Document): CommitData = gson.fromJson(document.toJson(), CommitData::class.java)
    }
}

@Serializable
data class Commits (
    private val data: List<CommitData>
)

class CommitsDatabaseService(
    private val database: MongoDatabase
) {
    private val commits: MongoCollection<Document>

    init {
        this.database.createCollection("commits")
        this.commits = database.getCollection("commits")
    }

    suspend fun getCommitsOf(branch: String): Commits = withContext(Dispatchers.IO) {
        val commitsList = commits.find(Filters.eq("branch_name", branch)).map { CommitData.fromDocument(it) }.toList()

        return@withContext Commits(commitsList)
    }

    suspend fun add(branch: String, commitData: CommitData) = withContext(Dispatchers.IO) {
        val commitDoc = commitData.toDocument()
        commits.insertOne(commitDoc)
    }

    suspend fun get(branch: String): Commits? = withContext(Dispatchers.IO) {
        val found = commits.find(Filters.eq("branch_name", branch)).map { CommitData.fromDocument(it) }
        val allCommitsList = found.toList()

        return@withContext Commits(allCommitsList)
    }

    suspend fun delete(branch: String): Document? = withContext(Dispatchers.IO) {
        commits.findOneAndDelete(Filters.eq("branch_name", branch))
    }
}