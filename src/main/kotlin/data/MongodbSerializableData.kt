package moe.luminolmc.data

import org.bson.Document

interface MongodbSerializableData {
    fun toDocument(): Document
}