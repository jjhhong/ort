/*
 * Copyright (C) 2020 HERE Europe B.V.
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

package org.ossreviewtoolkit.reporter.reporters

import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler

import java.io.File

import kotlin.reflect.full.memberProperties

import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.licenses.LicenseView
import org.ossreviewtoolkit.model.licenses.ResolvedLicense
import org.ossreviewtoolkit.model.licenses.ResolvedLicenseFileInfo
import org.ossreviewtoolkit.model.licenses.ResolvedLicenseInfo
import org.ossreviewtoolkit.reporter.Reporter
import org.ossreviewtoolkit.reporter.ReporterInput

class NoticeTemplateReporter : Reporter {
    override val reporterName = "NoticeTemplateReporter"

    private val noticeFilename = "NOTICE"

    override fun generateReport(input: ReporterInput, outputDir: File, options: Map<String, String>): List<File> {
        val freemarkerConfig = Configuration(Configuration.VERSION_2_3_30).apply {
            defaultEncoding = "UTF-8"
            fallbackOnNullLoopVariable = false
            logTemplateExceptions = true
            tagSyntax = Configuration.SQUARE_BRACKET_TAG_SYNTAX
            templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
            // TODO: Switch to FileTemplateLoader below for file option.
            templateLoader = ClassTemplateLoader(this@NoticeTemplateReporter.javaClass.classLoader, "template.notice")
            wrapUncheckedExceptions = true
        }

        val projects = input.ortResult.getProjects().map { project ->
            PackageNoticeModel(project.id, input)
        }

        val packages = input.ortResult.getPackages().map { pkg ->
            PackageNoticeModel(pkg.pkg.id, input)
        }

        val dataModel = mapOf(
            "projects" to projects,
            "packages" to packages,
            "licenseTextProvider" to input.licenseTextProvider,
            "helper" to NoticeHelper()
        )

        val outputFile = outputDir.resolve(noticeFilename)

        val template = freemarkerConfig.getTemplate("default.ftlh")
        outputFile.writer().use {
            template.process(dataModel, it)
        }

        return listOf(outputFile)
    }

    /**
     * License information for a single package or project.
     */
    class PackageNoticeModel(
        val id: Identifier,
        private val input: ReporterInput
    ) {
        /**
         * True if the package is excluded.
         */
        val excluded: Boolean by lazy { input.ortResult.isExcluded(id) }

        /**
         * The resolved license information for the package.
         */
        val license: ResolvedLicenseInfo by lazy { input.licenseInfoResolver.resolveLicenseInfo(id) }

        /**
         * The resolved license file information for the package.
         */
        val licenseFiles: ResolvedLicenseFileInfo by lazy { input.licenseInfoResolver.resolveLicenseFiles(id) }

        /**
         * Returns all [ResolvedLicense]s for this package excluding those licenses which are contained in any of the
         * license files. This is useful when the raw texts of the license files are included in the generated notice
         * file and all licenses not contained in those files shall be listed separately.
         */
        fun licensesNotInLicenseFiles(): List<ResolvedLicense> {
            val noticeFileLicenses = licenseFiles.files.flatMap { it.licenses }
            return license.filter { it !in noticeFileLicenses }
        }
    }

    /**
     * A collection of helper functions for the Freemarker templates.
     */
    class NoticeHelper {
        /**
         * Return a [LicenseView] constant by name to make them easily available to the Freemarker templates.
         */
        fun licenseView(name: String): LicenseView =
            LicenseView.Companion::class.memberProperties
                .first { it.name == name }
                .get(LicenseView.Companion) as LicenseView

        /**
         * Merge the [ResolvedLicense]s of multiple [models] and filter them using [licenseView].
         */
        @JvmOverloads
        fun mergeLicenses(
            models: Collection<PackageNoticeModel>,
            licenseView: LicenseView = LicenseView.ALL,
            omitExcluded: Boolean = true
        ): List<ResolvedLicense> {
            val allLicenses = models
                .filter { !omitExcluded || !it.excluded }
                .flatMap { it.license.filter(licenseView).licenses }
                .groupBy { it.license }
            return allLicenses.map { (_, licenses) -> licenses.merge() }
        }
    }
}

fun List<ResolvedLicense>.merge(): ResolvedLicense {
    require(this.isNotEmpty()) { "Cannot not merge an empty list." }
    return ResolvedLicense(
        license = first().license,
        sources = flatMapTo(mutableSetOf()) { it.sources },
        originalDeclaredLicenses = flatMapTo(mutableSetOf()) { it.originalDeclaredLicenses },
        locations = flatMapTo(mutableSetOf()) { it.locations }
    )
}
