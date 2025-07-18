package moe.luminolmc.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

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
    val branchName: String,
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
    private val database: MongoDatabase,
    private val projectsDatabaseService: ProjectsDatabaseService
) {
    private val commitsOfEachProject: MutableMap<String, MongoCollection<Document>> = ConcurrentHashMap()

    init {
        this.database.createCollection("commits")

        runBlocking {
            val allProjects = projectsDatabaseService.getAllProjects()

            for (projectInfo in allProjects) {
                commitsOfEachProject.computeIfAbsent(projectInfo.name) {
                    return@computeIfAbsent database.getCollection("commits_of_$it")
                }
            }
        }
    }

    suspend fun getCommitsOf(project: String, branch: String): Commits = withContext(Dispatchers.IO) {
        val commitsList = commitsOfEachProject[project]?.find(Filters.eq("branch_name", branch))?.map { CommitData.fromDocument(it) }
            ?.toList()

        if (commitsList == null) {
            return@withContext Commits(emptyList())
        }

        return@withContext Commits(commitsList)
    }

    suspend fun add(project: String, commitData: CommitData) = withContext(Dispatchers.IO) {
        val commitDoc = commitData.toDocument()
        val collection = commitsOfEachProject[project]

        collection?.insertOne(commitDoc)
    }

    suspend fun get(project: String, branch: String): Commits? = withContext(Dispatchers.IO) {
        val found = commitsOfEachProject[project]?.find(Filters.eq("branch_name", branch))?.map { CommitData.fromDocument(it) }
        val allCommitsList = found?.toList() ?: emptyList()

        return@withContext Commits(allCommitsList)
    }

    suspend fun delete(project: String, branch: String): Document? = withContext(Dispatchers.IO) {
        commitsOfEachProject[project]?.findOneAndDelete(Filters.eq("branch_name", branch))
    }
}