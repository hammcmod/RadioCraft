<#
.SYNOPSIS
Push a mod's commits to GitHub, or build, tag and publish a version to Modrinth and GitHub.

.DESCRIPTION
Two modes. Pushing code is not a release: releases go out only when asked for, so players are not sent a new version
for every push.

    tools\push-versions.ps1 -PushOnly                        # push the branch (and its tags) to GitHub; no release
    tools\push-versions.ps1 -Version 1.9.1                   # release: push to GitHub, publish on Modrinth and as a
                                                             # GitHub release with the jar, notes from doc/ReleaseNotes-1.9.md
    tools\push-versions.ps1 -Version 1.9.1 -Tag -Build       # tag the current commit 1.9.1, push it, build, release
    tools\push-versions.ps1 -Version 1.9.1 -NoGitHubRelease  # release on Modrinth only (the code is still pushed)
    tools\push-versions.ps1 -Project anotherweathermod -Version 1.9.0   # AWM (GitHub only, not on Modrinth)
    tools\push-versions.ps1 -Version 1.9.1 -DryRun           # show what would be pushed and sent

The GitHub repository is the project's "github" entry in tools/modrinth_projects.json (RadioCraft:
RadiocraftARRL/RadioCraft); pushes go there whatever the local `origin` remote says.

Tokens come from the environment (MODRINTH_TOKEN, GITHUB_TOKEN) or from ~/.radiocraft/modrinth_token and
~/.radiocraft/github_token; -ModrinthToken / -GitHubToken set them for this run. Never commit a token.

Projects, their Modrinth ids and dependency lists live in tools/modrinth_projects.json.

.PARAMETER Version
The version number, e.g. 1.9.1. Defaults to the git tag on HEAD (`git describe --tags --exact-match`).
.PARAMETER Type
alpha (default), beta or release. Anything but release is marked a pre-release on GitHub.
.PARAMETER Notes
Markdown changelog. Defaults to the project's "notes" pattern in modrinth_projects.json (for RadioCraft,
doc/ReleaseNotes-<major.minor>.md); the "# " section naming the version is used.
.PARAMETER Jar
The jar to upload. Defaults to build/libs/<jar_prefix><Version>.jar.
.PARAMETER Build
Run `gradlew build` first (unit tests and the jar check included).
.PARAMETER SkipLaunchTest
Do not launch the jar before publishing. A project with "launch_test" in modrinth_projects.json (RadioCraft) otherwise
starts the exact jar being uploaded on a production NeoForge server and client first (tools/prod_launch_test.py),
and refuses to publish it if either fails to load.
.PARAMETER PushOnly
Only push the current branch and its tags to the project's GitHub repository. Nothing is built or released.
.PARAMETER Tag
Create the git tag <Version> on HEAD (refusing if the tree is dirty) before pushing.
.PARAMETER NoGitHubRelease
Release on Modrinth only, without a GitHub release. The branch and tag are still pushed.
.PARAMETER GitHub
Kept for old command lines; a release makes a GitHub release by default now.
.PARAMETER NoModrinth
Skip Modrinth (GitHub only).
.PARAMETER Featured
Mark the Modrinth version as featured.
#>
[CmdletBinding()]
param(
    [string]$Project = "radiocraft",
    [string]$Version,
    [ValidateSet("alpha", "beta", "release")][string]$Type = "alpha",
    [string]$Name,
    [string]$Notes,
    [string]$Jar,
    [switch]$Build,
    [switch]$PushOnly,
    [switch]$Tag,
    [switch]$GitHub,
    [switch]$NoGitHubRelease,
    [switch]$NoModrinth,
    [switch]$Featured,
    [switch]$SkipLaunchTest,
    [switch]$DryRun,
    [string]$ModrinthToken,
    [string]$GitHubToken
)

$ErrorActionPreference = "Stop"
$toolsDir = $PSScriptRoot
$projects = Get-Content (Join-Path $toolsDir "modrinth_projects.json") -Raw | ConvertFrom-Json
$cfg = $projects.$Project
if (-not $cfg) { throw "Unknown project '$Project'. Known: $(($projects.PSObject.Properties.Name | Where-Object { $_ -notlike '_*' }) -join ', ')" }

$root = Resolve-Path (Join-Path (Join-Path $toolsDir "..") $cfg.root)
Push-Location $root
try {
    if ($ModrinthToken) { $env:MODRINTH_TOKEN = $ModrinthToken }
    if ($GitHubToken) { $env:GITHUB_TOKEN = $GitHubToken }

    $remote = "https://github.com/$($cfg.github).git"

    # Pushes the current branch (when on one; a release worktree sits on a detached tag) and the tag $pushTag, if any,
    # to the project's GitHub repository.
    function Push-ToGitHub([string]$pushTag) {
        $refs = @()
        $branch = git symbolic-ref --quiet --short HEAD 2>$null
        if ($branch) { $refs += "HEAD:refs/heads/$branch" }
        if ($pushTag) { $refs += "refs/tags/$($pushTag):refs/tags/$pushTag" }
        if (-not $refs) { Write-Host "Nothing to push (detached HEAD and no tag)."; return }
        Write-Host "Pushing $($refs -join ', ') to $remote"
        if ($DryRun) { return }
        git push $remote @refs
        if ($LASTEXITCODE) { throw "git push to $remote failed" }
    }

    if ($PushOnly) {
        $branch = git symbolic-ref --quiet --short HEAD 2>$null
        if (-not $branch) { throw "-PushOnly needs a branch checked out (HEAD is detached)." }
        Write-Host "Pushing $branch and its tags to $remote (no release)"
        if (-not $DryRun) {
            git push $remote "HEAD:refs/heads/$branch" --follow-tags
            if ($LASTEXITCODE) { throw "git push to $remote failed" }
        }
        return
    }

    if (-not $Version) {
        $Version = (git describe --tags --exact-match 2>$null)
        if (-not $Version) { throw "No -Version given and HEAD is not tagged. Pass -Version 1.9.1 (and -Tag to tag it)." }
        $Version = $Version.TrimStart("v")
    }

    if ($Tag) {
        $dirty = git status --porcelain
        if ($dirty) { throw "The working tree has uncommitted changes; commit them before tagging $Version." }
        if (git tag --list $Version) { Write-Host "Tag $Version already exists, keeping it." }
        else { git tag $Version; if ($LASTEXITCODE) { throw "git tag failed" } }
    }

    if ($Build) {
        Write-Host "Building $Project $Version..."
        & .\gradlew build -q
        if ($LASTEXITCODE) { throw "gradlew build failed" }
    }

    if (-not $Jar) { $Jar = Join-Path "build/libs" ("{0}{1}.jar" -f $cfg.jar_prefix, $Version) }
    if (-not (Test-Path $Jar)) { throw "No jar at $Jar. Is HEAD at the $Version tag? Build with -Build, or pass -Jar." }

    if ($cfg.launch_test -and -not $SkipLaunchTest) {
        Write-Host "Launching $Jar on a production NeoForge server and client..."
        $jarPath = (Resolve-Path $Jar).Path
        & .\gradlew $cfg.launch_test "-PlaunchJar=$jarPath" --console=plain
        if ($LASTEXITCODE) { throw "The launch test failed: $Jar does not load outside the dev environment. Not publishing." }
    }

    if (-not $Notes) {
        $majorMinor = ($Version -split "\.")[0..1] -join "."
        $Notes = $cfg.notes.Replace("{version}", $Version).Replace("{major_minor}", $majorMinor)
    }
    if (-not (Test-Path $Notes)) { throw "No release notes at $Notes; write them or pass -Notes." }

    # A release always pushes its code and tag first, so the GitHub release and the Modrinth version point at
    # commits that are on GitHub.
    if (git tag --list $Version) { Push-ToGitHub $Version } else { Push-ToGitHub $null }

    $args = @("-I", (Join-Path $toolsDir "modrinth_release.py"),
              "--project", $Project, "--version", $Version, "--jar", $Jar, "--changelog", $Notes, "--type", $Type)
    if ($Name) { $args += @("--name", $Name) }
    if (-not $NoGitHubRelease) { $args += "--github" }
    if ($NoModrinth) { $args += "--no-modrinth" }
    if ($Featured) { $args += "--featured" }
    if ($DryRun) { $args += "--dry-run" }

    & python @args
    if ($LASTEXITCODE) { throw "publishing failed" }
}
finally {
    Pop-Location
}
