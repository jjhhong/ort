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

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import org.ossreviewtoolkit.model.Identifier

import java.io.File

import org.ossreviewtoolkit.model.OrtResult
import org.ossreviewtoolkit.model.config.CopyrightGarbage
import org.ossreviewtoolkit.model.config.FileArchiverConfiguration
import org.ossreviewtoolkit.model.config.FileStorageConfiguration
import org.ossreviewtoolkit.model.config.LocalFileStorageConfiguration
import org.ossreviewtoolkit.model.config.OrtConfiguration
import org.ossreviewtoolkit.model.config.ScannerConfiguration
import org.ossreviewtoolkit.model.licenses.LicenseView
import org.ossreviewtoolkit.reporter.ORT_RESULT
import org.ossreviewtoolkit.reporter.ReporterInput
import org.ossreviewtoolkit.utils.LICENSE_FILENAMES
import org.ossreviewtoolkit.utils.ORT_NAME

class NoticeTemplateReporterTest : WordSpec({
    "NoticeTemplateReporter" should {
        "generate the correct license notes" {
            val expectedText = File("src/funTest/assets/notice-template-reporter-expected-results").readText()

            val report = generateReport(ORT_RESULT)

            report shouldBe expectedText
        }

        "generate the correct license notes with archived license files" {
            val expectedText =
                File("src/funTest/assets/notice-template-reporter-expected-results-with-license-files").readText()

            val archiveDir = File("src/funTest/assets/archive")
            val config = OrtConfiguration(
                ScannerConfiguration(
                    archive = FileArchiverConfiguration(
                        patterns = LICENSE_FILENAMES,
                        storage = FileStorageConfiguration(
                            localFileStorage = LocalFileStorageConfiguration(
                                directory = archiveDir,
                                compression = false
                            )
                        )
                    )
                )
            )
            val report = generateReport(ORT_RESULT, config)

            val id = Identifier("NPM:@ort:concluded-license:1.0")
            val input = ReporterInput(
                ORT_RESULT,
                config,
                copyrightGarbage = CopyrightGarbage()
            )

            val resolved = input.licenseInfoResolver.resolveLicenseInfo(id)
            val filtered = resolved.filter(LicenseView.CONCLUDED_OR_REST)

            filtered.licenses.size shouldBe 1

            report shouldBe expectedText
        }
    }
})

private fun generateReport(
    ortResult: OrtResult,
    config: OrtConfiguration = OrtConfiguration(),
    copyrightGarbage: CopyrightGarbage = CopyrightGarbage()
): String {
    val input = ReporterInput(
        ortResult,
        config,
        copyrightGarbage = copyrightGarbage
    )

    val outputDir = createTempDir(ORT_NAME, NoticeTemplateReporterTest::class.simpleName).apply { deleteOnExit() }

    return NoticeTemplateReporter().generateReport(input, outputDir).single().readText()
}
