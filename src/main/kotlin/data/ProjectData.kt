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
import org.bson.types.ObjectId

@Serializable
data class ProjectData(
    @SerializedName("project_name")
    val name: String,
    @SerializedName("branches")
    val branches: List<BranchInfo>
) : MongodbSerializableData{
    override fun toDocument(): Document = Document.parse(gson.toJson(this))

    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

        fun fromDocument(document: Document): ProjectData = gson.fromJson(document.toJson(), ProjectData::class.java)
    }
}

class ProjectsDatabaseService(
    private val database: MongoDatabase
) {
    private val projects: MongoCollection<Document>

    init {
        this.database.createCollection("projects")
        this.projects = database.getCollection("projects")
    }

    suspend fun getAllProjects(): List<ProjectData> = withContext(Dispatchers.IO) {
        return@withContext projects.find().toList().map { ProjectData.fromDocument(it) }
    }

    suspend fun create(project: ProjectData): String = withContext(Dispatchers.IO) {
        val doc = project.toDocument()
        projects.insertOne(doc)
        doc["_id"].toString()
    }

    suspend fun get(name: String): ProjectData? = withContext(Dispatchers.IO) {
        projects.find(Filters.eq("project_name", name)).first()?.let(ProjectData::fromDocument)
    }

    suspend fun update(name: String, project: ProjectData): Document? = withContext(Dispatchers.IO) {
        projects.findOneAndReplace(Filters.eq("project_name", name), project.toDocument())
    }

    suspend fun delete(name: String): Document? = withContext(Dispatchers.IO) {
        projects.findOneAndDelete(Filters.eq("project_name", ObjectId(name)))
    }

    suspend fun hasProject(name: String): Boolean{
        return projects.find(Filters.eq("project_name", name)).first() != null
    }
}