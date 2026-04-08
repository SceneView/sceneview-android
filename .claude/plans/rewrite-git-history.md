# Task: Rewrite git history — Thomas as author, Claude as co-author

## Context
All commits in session 26 (dbc7842..HEAD) were authored as "Claude" but should be:
- **Author:** Thomas Gorisse <AjaxMusic@gmail.com>
- **Co-authored-by:** Claude <noreply@anthropic.com>

## Command to execute (DESTRUCTIVE — requires force-push)

```bash
# Rewrite all commits from session 26 start to HEAD
git filter-branch --env-filter '
if [ "$GIT_AUTHOR_NAME" = "Claude" ]; then
    export GIT_AUTHOR_NAME="Thomas Gorisse"
    export GIT_AUTHOR_EMAIL="AjaxMusic@gmail.com"
fi
if [ "$GIT_COMMITTER_NAME" = "Claude" ]; then
    export GIT_COMMITTER_NAME="Thomas Gorisse"
    export GIT_COMMITTER_EMAIL="AjaxMusic@gmail.com"
fi
' dbc7842^..HEAD

# Force push (DANGEROUS — coordinate with team first)
git push --force-with-lease origin main
```

## Alternative: git rebase with exec

```bash
git rebase -i dbc7842^ --exec 'git commit --amend --author="Thomas Gorisse <AjaxMusic@gmail.com>" --no-edit'
git push --force-with-lease origin main
```

## Prerequisites
- [ ] CI must be green first (fix compilation errors from sample rewrites)
- [ ] No other contributors working on main
- [ ] Backup current state: `git branch backup-before-rewrite`

## After rewrite
- All ~65 commits will show Thomas as author
- Co-authored-by trailer should be added to each commit message
- GitHub will properly attribute the work to Thomas's profile
