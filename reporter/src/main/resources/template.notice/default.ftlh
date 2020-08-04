[#--
Copyright (C) 2020 HERE Europe B.V.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

SPDX-License-Identifier: Apache-2.0
License-Filename: LICENSE
--]
[#noautoesc]
[#-- TODO: Omit excluded projects. --]
[#if projects?has_content ]
This software includes external packages and source code.
The applicable license information is listed below:

----
[#assign mergedLicenses = helper.mergeLicenses(projects, helper.licenseView("CONCLUDED_OR_REST"))]
[#list mergedLicenses as resolvedLicense]
[#if !resolvedLicense?is_first]
  --
[/#if]
[#assign copyrights = resolvedLicense.getCopyrights(true)]
[#list copyrights as copyright]
[#if copyright?is_first]

[/#if]
${copyright}
[/#list]
[#assign licenseText = licenseTextProvider.getLicenseText(resolvedLicense.license.toString())]
[#if licenseText??]

${licenseText}
[/#if]
[/#list]
----
[/#if]

[#if packages?has_content ]
This project contains or depends on third-party software components pursuant to the following licenses:
[/#if]

[#list packages as package]
[#if !package.excluded]
----

Package: ${package.id.namespace}:${package.id.name}:${package.id.version}
[#assign resolvedLicenses = package.license.filter(helper.licenseView("CONCLUDED_OR_REST")).licenses]
[#if resolvedLicenses?has_content]

The following copyrights and licenses were found in the source code of this package:
[/#if]
[#list resolvedLicenses as resolvedLicense]
[#if !resolvedLicense?is_first]
  --
[/#if]
[#assign copyrights = resolvedLicense.getCopyrights(true)]
[#list copyrights as copyright]
[#if copyright?is_first]

[/#if]
${copyright}
[/#list]
[#assign licenseText = licenseTextProvider.getLicenseText(resolvedLicense.license.toString())]
[#if licenseText??]

${licenseText}
[/#if]
[/#list]
[/#if]
[/#list]
[/#noautoesc]
