#!/bin/bash
set -e

echo "=========================================="
echo "Tekton + Argo CD GitOps Demo Setup"
echo "=========================================="
echo ""

# Check if logged in to OpenShift
if ! oc whoami &>/dev/null; then
    echo "❌ Error: Not logged in to OpenShift"
    echo "Please run: oc login <cluster-url>"
    exit 1
fi

echo "✓ Logged in as: $(oc whoami)"
echo ""

# Step 1: Create project
echo "Step 1: Creating OpenShift project..."
if ! oc get project tekton-argocd-demo &>/dev/null; then
    oc new-project tekton-argocd-demo
    echo "✓ Project 'tekton-argocd-demo' created"
else
    echo "✓ Project 'tekton-argocd-demo' already exists"
    oc project tekton-argocd-demo
fi
echo ""

# Step 2: Apply Tekton Tasks
echo "Step 2: Creating Tekton Tasks..."
oc apply -f tekton/tasks/git-clone.yaml
oc apply -f tekton/tasks/maven-build.yaml
oc apply -f tekton/tasks/buildah-push.yaml
oc apply -f tekton/tasks/update-gitops-manifest.yaml
echo "✓ Tekton Tasks created"
echo ""

# Step 3: Apply Tekton Pipeline
echo "Step 3: Creating Tekton Pipeline..."
oc apply -f tekton/pipeline/build-update-gitops-pipeline.yaml
echo "✓ Tekton Pipeline created"
echo ""

# Step 4: Apply Tekton Triggers
echo "Step 4: Creating Tekton Triggers..."
oc apply -f tekton/triggers/trigger-binding.yaml
oc apply -f tekton/triggers/trigger-template.yaml
oc apply -f tekton/triggers/event-listener.yaml
oc apply -f tekton/triggers/event-listener-route.yaml
echo "✓ Tekton Triggers created"
echo ""

# Step 5: Configure SCC for buildah
echo "Step 5: Configuring SecurityContextConstraints..."
SCC_CONFIGURED=false
if oc adm policy add-scc-to-user privileged -z pipeline 2>/dev/null; then
    echo "✓ Privileged SCC granted to pipeline serviceaccount"
    SCC_CONFIGURED=true
else
    echo "⚠ Could not configure SCC (requires admin privileges)"
    echo "  Please run manually:"
    echo "  oc adm policy add-scc-to-user privileged -z pipeline"
    SCC_CONFIGURED=false
fi
echo ""

# Step 6: Create Git Secret for pushing to GitOps repo
echo "Step 6: Configuring Git credentials..."
echo "⚠ Git credentials are required for Tekton to push GitOps manifest updates"
echo ""
echo "You need to create a GitHub Personal Access Token with 'repo' scope:"
echo "  1. Go to https://github.com/settings/tokens"
echo "  2. Click 'Generate new token (classic)'"
echo "  3. Select scope: 'repo' (Full control of private repositories)"
echo "  4. Generate and copy the token"
echo ""
read -p "Do you want to configure Git credentials now? (y/n): " configure_git

if [ "$configure_git" = "y" ] || [ "$configure_git" = "Y" ]; then
    read -p "Enter your GitHub username: " git_username
    read -sp "Enter your GitHub Personal Access Token: " git_token
    echo ""

    # Create git-credentials secret
    oc create secret generic git-credentials \
        --from-literal=username="$git_username" \
        --from-literal=password="$git_token" \
        --dry-run=client -o yaml | oc apply -f -

    # Annotate secret for Tekton
    oc annotate secret git-credentials \
        "tekton.dev/git-0=https://github.com" --overwrite

    # Link secret to pipeline serviceaccount
    oc secrets link pipeline git-credentials

    echo "✓ Git credentials configured"
else
    echo "⚠ Skipped Git credentials configuration"
    echo "  You can configure it later with:"
    echo "  oc create secret generic git-credentials --from-literal=username=<username> --from-literal=password=<token>"
    echo "  oc annotate secret git-credentials 'tekton.dev/git-0=https://github.com'"
    echo "  oc secrets link pipeline git-credentials"
fi
echo ""

# Step 7: Apply Argo CD Application
echo "Step 7: Creating Argo CD Application..."
if oc get namespace openshift-gitops &>/dev/null; then
    oc apply -f argocd/application.yaml
    echo "✓ Argo CD Application created"
    echo ""
    echo "View Argo CD Application:"
    ARGOCD_ROUTE=$(oc get route openshift-gitops-server -n openshift-gitops -o jsonpath='{.spec.host}' 2>/dev/null || echo "NOT_FOUND")
    if [ "$ARGOCD_ROUTE" != "NOT_FOUND" ]; then
        echo "  Argo CD URL: https://$ARGOCD_ROUTE"
        echo "  Username: admin"
        echo "  Password: $(oc get secret openshift-gitops-cluster -n openshift-gitops -o jsonpath='{.data.admin\.password}' 2>/dev/null | base64 -d || echo 'NOT_FOUND')"
    fi
else
    echo "⚠ OpenShift GitOps (Argo CD) is not installed"
    echo "  Please install Red Hat OpenShift GitOps Operator first"
fi
echo ""

# Step 8: Display Webhook URL
echo "Step 8: GitHub Webhook Configuration"
WEBHOOK_URL=$(oc get route github-webhook -o jsonpath='{.spec.host}' 2>/dev/null || echo "NOT_FOUND")
if [ "$WEBHOOK_URL" != "NOT_FOUND" ]; then
    echo "✓ Webhook URL: https://${WEBHOOK_URL}"
    echo ""
    echo "Configure GitHub Webhook:"
    echo "  1. Go to https://github.com/yKanaGit/Tekton-ArgoCD/settings/hooks"
    echo "  2. Click 'Add webhook'"
    echo "  3. Payload URL: https://${WEBHOOK_URL}"
    echo "  4. Content type: application/json"
    echo "  5. Select: Just the push event"
    echo "  6. Click 'Add webhook'"
else
    echo "⚠ Webhook route not found"
fi
echo ""

echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
echo "Summary:"
echo "  - Tekton Pipeline: build-update-gitops-pipeline"
echo "  - Argo CD Application: shipper-onboarding-api"
echo "  - Target namespace: tekton-argocd-demo"
echo ""
if [ "$SCC_CONFIGURED" = false ]; then
    echo "⚠ Action Required:"
    echo "  - Configure SCC: oc adm policy add-scc-to-user privileged -z pipeline"
fi
if [ "$configure_git" != "y" ] && [ "$configure_git" != "Y" ]; then
    echo "⚠ Action Required:"
    echo "  - Configure Git credentials (see above)"
fi
echo ""
echo "Next steps:"
echo "  1. Push a commit to trigger the pipeline"
echo "  2. Watch Tekton build and update GitOps manifest"
echo "  3. Watch Argo CD automatically sync and deploy"
echo ""
