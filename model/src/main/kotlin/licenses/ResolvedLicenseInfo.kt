/*
 * Copyright (C) 2017-2020 HERE Europe B.V.
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

package org.ossreviewtoolkit.model.licenses

import org.ossreviewtoolkit.model.CopyrightFinding
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.LicenseSource
import org.ossreviewtoolkit.model.Provenance
import org.ossreviewtoolkit.model.TextLocation
import org.ossreviewtoolkit.model.config.CopyrightGarbage
import org.ossreviewtoolkit.model.config.LicenseFindingCuration
import org.ossreviewtoolkit.model.config.PathExclude
import org.ossreviewtoolkit.spdx.SpdxSingleLicenseExpression
import org.ossreviewtoolkit.utils.CopyrightStatementsProcessor
import org.ossreviewtoolkit.utils.DeclaredLicenseProcessor

/**
 * Resolved license information about a package (or project).
 */
data class ResolvedLicenseInfo(
    /**
     * The identifier of the package (or project).
     */
    val id: Identifier,

    /**
     * The unresolved license info.
     */
    val licenseInfo: LicenseInfo,

    /**
     * The list of [ResolvedLicense]s for this package (or project).
     */
    val licenses: List<ResolvedLicense>,

    /**
     * All copyright findings with statements that are contained in [CopyrightGarbage], mapped to the [Provenance] where
     * they were detected.
     */
    val copyrightGarbage: Map<Provenance, Set<CopyrightFinding>>,

    /**
     * All copyright findings that could not be matched to a license finding, mapped to the [Provenance] where they were
     * detected.
     */
    val unmatchedCopyrights: Map<Provenance, Set<CopyrightFinding>>
) : Iterable<ResolvedLicense> by licenses {
    operator fun get(license: SpdxSingleLicenseExpression): ResolvedLicense? = find { it.license == license }

    /**
     * Call [LicenseView.filter] on this [ResolvedLicenseInfo].
     */
    fun filter(licenseView: LicenseView) = licenseView.filter(this)

    /**
     * Filter all licenses that have a location matching [provenance] and [path].
     */
    fun filter(provenance: Provenance, path: String): List<ResolvedLicense> =
        filter { resolvedLicense ->
            resolvedLicense.locations.any {
                it.provenance == provenance && it.location.path == path
            }
        }
}

/**
 * Resolved information for a single license.
 */
data class ResolvedLicense(
    /**
     * The license.
     */
    val license: SpdxSingleLicenseExpression,

    /**
     * The sources where this license was found.
     */
    val sources: Set<LicenseSource>,

    /**
     * The list of original declared license that were [processed][DeclaredLicenseProcessor] to this [license], or an
     * empty list, if this [license] was not modified during processing.
     */
    val originalDeclaredLicenses: Set<String>,

    /**
     * All text locations where this license was found.
     */
    val locations: Set<ResolvedLicenseLocation>
) {
    /**
     * True, if this license was [detected][LicenseSource.DETECTED] and all [locations] have matching path excludes.
     */
    val isDetectedExcluded by lazy {
        LicenseSource.DETECTED in sources && locations.all { it.matchingPathExcludes.isNotEmpty() }
    }

    fun getCopyrights(omitExcluded: Boolean = false): Set<String> =
        locations.flatMapTo(sortedSetOf()) { location ->
            location.copyrights.filter { copyright ->
                !omitExcluded || copyright.findings.any { it.matchingPathExcludes.isEmpty() }
            }.map { it.statement }
        }
}

/**
 * A resolved text location.
 */
data class ResolvedLicenseLocation(
    /**
     * The provenance of the file.
     */
    val provenance: Provenance,

    /**
     * The text location.
     */
    val location: TextLocation,

    /**
     * The applied [LicenseFindingCuration], or null if none were applied.
     */
    val appliedCuration: LicenseFindingCuration?,

    /**
     * All [PathExclude]s matching this [location].
     */
    val matchingPathExcludes: List<PathExclude>,

    /**
     * All copyright findings associated to this license location.
     */
    val copyrights: Set<ResolvedCopyright>
)

/**
 * A resolved copyright.
 */
data class ResolvedCopyright(
    /**
     * The resolved copyright statement.
     */
    val statement: String,

    /**
     * The resolved findings for this copyright. The statements in the findings can be different to [statement] if they
     * were processed by the [CopyrightStatementsProcessor].
     */
    val findings: Set<ResolvedCopyrightFinding>
)

/**
 * A resolved copyright finding.
 */
data class ResolvedCopyrightFinding(
    /**
     * The copyright statement.
     */
    val statement: String,

    /**
     * The location where this copyright was found.
     */
    val location: TextLocation,

    /**
     * All [PathExclude]s matching this [location].
     */
    val matchingPathExcludes: List<PathExclude>
)
