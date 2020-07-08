/*
 * Copyright (C) 2017-2019 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

import java.io.File

/**
 * An enumeration of supported file formats for (de-)serialization, their primary [fileExtension] and optional aliases
 * (not including the dot).
 */
enum class FileFormat(val mapper: ObjectMapper, val fileExtension: String, vararg aliases: String) {
    /**
     * Specifies the [JSON](http://www.json.org/) format.
     */
    JSON(jsonMapper, "json"),

    /**
     * Specifies the [XML](http://www.xml.org/) format.
     */
    XML(xmlMapper, "xml"),

    /**
     * Specifies the [YAML](http://yaml.org/) format.
     */
    YAML(yamlMapper, "yml", "yaml");

    companion object {
        /**
         * Return the [FileFormat] for the given [file], or `null` if there is none.
         */
        fun forFile(file: File): FileFormat =
            enumValues<FileFormat>().find {
                file.extension in it.fileExtensions
            } ?: throw IllegalArgumentException(
                "Unsupported file format '${file.extension}' of file '${file.absolutePath}'."
            )
    }

    /**
     * The list of file extensions used by this file format.
     */
    val fileExtensions = listOf(fileExtension, *aliases)
}

/**
 * Get the Jackson [ObjectMapper] for this file based on the file extension configured in [FileFormat.mapper].
 *
 * @throws IllegalArgumentException If no matching [FileFormat] for the [File.extension] can be found.
 */
fun File.mapper() = FileFormat.forFile(this).mapper

/**
 * Use the Jackson mapper returned from [File.mapper] to read an object of type [T] from this file.
 */
inline fun <reified T : Any> File.readValue(): T = mapper().readValue(this)