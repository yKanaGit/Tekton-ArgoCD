# Tekton + Argo CD GitOps Demo

OpenShift Pipelines (Tekton) と OpenShift GitOps (Argo CD) を組み合わせた GitOps デモです。

## 📦 リポジトリ

https://github.com/yKanaGit/Tekton-ArgoCD

## 🎯 概要

このデモでは、Tekton と Argo CD の責務を明確に分離した GitOps アーキテクチャを実装しています。

### 責務分担

**Tekton (CI):**
- ソースコードのビルド
- コンテナイメージのビルド＆プッシュ
- GitOps マニフェストの更新（イメージタグ）
- Git への commit & push

**Argo CD (CD / GitOps):**
- Git リポジトリの監視
- Kubernetes / OpenShift クラスターへの同期
- Self-heal / Prune による GitOps 管理

### 既存の Tekton-only 版との違い

| 項目 | Tekton-only 版 | Tekton + Argo CD 版 (本デモ) |
|------|---------------|----------------------------|
| **デプロイ方法** | Tekton から `oc apply` で直接デプロイ | Argo CD が Git を監視して自動同期 |
| **マニフェスト管理** | Git で管理されるが、デプロイは Tekton が実行 | Git が Single Source of Truth |
| **ドリフト検知** | なし | Argo CD が自動検知・修正 (self-heal) |
| **ロールバック** | 手動で PipelineRun を再実行 | Git の履歴を revert するだけ |
| **可視性** | Tekton Console のみ | Tekton Console + Argo CD UI |

## 🏗️ アーキテクチャ

```
GitHub Push Event
       ↓
EventListener (Webhook)
       ↓
TriggerBinding + TriggerTemplate
       ↓
PipelineRun
       ↓
┌──────────────────────────────────────┐
│ 1. git-clone                         │
│    - GitHub からソースコードを取得    │
├──────────────────────────────────────┤
│ 2. maven-build                       │
│    - Maven で JAR をビルド           │
├──────────────────────────────────────┤
│ 3. buildah-push                      │
│    - コンテナイメージをビルド＆プッシュ│
├──────────────────────────────────────┤
│ 4. update-gitops-manifest            │
│    - kustomization.yaml の image tag  │
│      を更新して Git に push          │
└──────────────────────────────────────┘
       ↓
  Git Repository
  (k8s/overlays/dev/kustomization.yaml が更新される)
       ↓
  Argo CD が変更を検知
       ↓
  自動的に OpenShift クラスターに同期
       ↓
  アプリケーションがデプロイされる
```

## 📂 ディレクトリ構成

```
Tekton-ArgoCD/
├── README.md
├── setup.sh
├── app/                    # Shipper Onboarding API (Quarkus)
│   ├── pom.xml
│   └── src/
├── k8s/                    # Kustomize マニフェスト
│   ├── base/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── route.yaml
│   │   ├── postgresql-deployment.yaml
│   │   ├── postgresql-secret.yaml
│   │   └── kustomization.yaml
│   └── overlays/
│       └── dev/
│           └── kustomization.yaml   # ← Tekton がこのファイルを更新
├── tekton/
│   ├── tasks/
│   │   ├── git-clone.yaml
│   │   ├── maven-build.yaml
│   │   ├── buildah-push.yaml
│   │   └── update-gitops-manifest.yaml  # ← GitOps manifest 更新タスク
│   ├── pipeline/
│   │   └── build-update-gitops-pipeline.yaml
│   └── triggers/
│       ├── trigger-binding.yaml
│       ├── trigger-template.yaml
│       ├── event-listener.yaml
│       └── event-listener-route.yaml
└── argocd/
    └── application.yaml    # Argo CD Application 定義
```

## 📋 前提条件

- OpenShift クラスターへのアクセス（OpenShift 4.x 以上）
- **Red Hat OpenShift Pipelines Operator** がインストール済み
- **Red Hat OpenShift GitOps Operator** がインストール済み
- `oc` CLI がインストール済み
- Git がインストール済み
- **GitHub Personal Access Token**（`repo` スコープ）
  - Tekton が GitOps マニフェストを Git に push するために必要

## 🚀 セットアップ手順

### 1. リポジトリをクローン

```bash
git clone https://github.com/yKanaGit/Tekton-ArgoCD.git
cd Tekton-ArgoCD
```

### 2. OpenShift にログイン

```bash
oc login <your-cluster-url>
oc whoami
```

### 3. セットアップスクリプトを実行

```bash
./setup.sh
```

スクリプトは以下を実行します：

1. ✅ プロジェクト作成 (`tekton-argocd-demo`)
2. ✅ Tekton Tasks の作成
3. ✅ Tekton Pipeline の作成
4. ✅ Tekton Triggers の作成
5. ✅ SCC 設定（buildah 用）
6. ✅ Git 認証情報の設定（対話式）
7. ✅ Argo CD Application の作成
8. ✅ Webhook URL の表示

### 4. GitHub Personal Access Token の作成

Tekton が GitOps マニフェストを Git に push するために必要です。

1. https://github.com/settings/tokens にアクセス
2. **Generate new token (classic)** をクリック
3. **Scope を選択:**
   - ✅ `repo` (Full control of private repositories)
4. トークンを生成してコピー
5. setup.sh 実行時にトークンを入力

### 5. GitHub Webhook の設定

setup.sh が表示する Webhook URL を使用して、GitHub Webhook を設定します。

1. https://github.com/yKanaGit/Tekton-ArgoCD/settings/hooks にアクセス
2. **Add webhook** をクリック
3. 以下を設定：
   - **Payload URL**: `https://<webhook-url>` (setup.sh で表示された URL)
   - **Content type**: `application/json`
   - **SSL verification**: Enable SSL verification
   - **Which events**: Just the push event
   - **Active**: チェックを入れる
4. **Add webhook** をクリック

### 6. Argo CD UI にアクセス

```bash
# Argo CD の URL を取得
oc get route openshift-gitops-server -n openshift-gitops -o jsonpath='{.spec.host}'

# Argo CD の admin パスワードを取得
oc get secret openshift-gitops-cluster -n openshift-gitops -o jsonpath='{.data.admin\.password}' | base64 -d
```

ブラウザで Argo CD URL にアクセスし、`admin` ユーザーでログインします。

## 🧪 動作確認

### 方法1: 自動トリガー（推奨）

GitHub に変更をプッシュすると、自動的にパイプラインが実行されます：

```bash
# 簡単な変更を加える
echo "# GitOps Test" >> test.txt
git add test.txt
git commit -m "Test GitOps pipeline"
git push origin main
```

**フロー:**
1. GitHub Webhook が Tekton EventListener をトリガー
2. Tekton がビルド・イメージプッシュ・GitOps マニフェスト更新を実行
3. Tekton が `k8s/overlays/dev/kustomization.yaml` の `newTag` を更新して Git に push
4. Argo CD が Git の変更を検知
5. Argo CD が OpenShift クラスターに自動同期
6. アプリケーションがデプロイされる

### 確認方法

#### Tekton Pipeline の確認

```bash
# PipelineRun 一覧
oc get pipelinerun

# 最新の PipelineRun のログ
tkn pipelinerun logs --last -f
```

#### Argo CD Application の確認

Argo CD UI で `shipper-onboarding-api` Application を開くと、以下が確認できます：

- Sync Status: Git との同期状態
- Health Status: デプロイされたリソースの健全性
- Resource Tree: デプロイされたリソースの可視化

#### アプリケーションへアクセス

```bash
# Route URL を取得
APP_URL=$(oc get route shipper-onboarding-api -n tekton-argocd-demo -o jsonpath='{.spec.host}')
echo "Application URL: https://${APP_URL}"

# ヘルスチェック
curl https://${APP_URL}/q/health/live

# Shipper API
curl https://${APP_URL}/api/shippers
```

## 🔧 GitOps のメリット

### 1. Git が Single Source of Truth

すべてのデプロイメント状態が Git に記録されます。

```bash
# kustomization.yaml の履歴を確認
git log --oneline k8s/overlays/dev/kustomization.yaml
```

### 2. Self-Heal（自己修復）

誰かが手動で Deployment を変更しても、Argo CD が自動的に Git の状態に戻します。

```bash
# 試しに手動で Deployment を変更
oc scale deployment shipper-onboarding-api --replicas=3 -n tekton-argocd-demo

# Argo CD が自動的に replicas=1 に戻す（数秒後）
oc get deployment shipper-onboarding-api -n tekton-argocd-demo
```

### 3. Prune（不要なリソースの削除）

Git から削除されたリソースは、クラスターからも自動的に削除されます。

### 4. 簡単なロールバック

Git の履歴を revert するだけで、アプリケーションをロールバックできます。

```bash
# Git の履歴を確認
git log --oneline k8s/overlays/dev/kustomization.yaml

# 特定のコミットに戻す
git revert <commit-sha>
git push origin main

# Argo CD が自動的に古いバージョンにロールバック
```

## 🛠️ トラブルシューティング

### Tekton Pipeline が Git に push できない

**原因:** Git 認証情報が設定されていない

**解決方法:**

```bash
# GitHub Personal Access Token を使用して Secret を作成
oc create secret generic git-credentials \
  --from-literal=username=<your-github-username> \
  --from-literal=password=<your-github-token>

# Secret に annotation を追加
oc annotate secret git-credentials "tekton.dev/git-0=https://github.com"

# pipeline serviceaccount に Secret をリンク
oc secrets link pipeline git-credentials
```

### Argo CD が同期しない

**確認事項:**

1. Argo CD Application のステータスを確認

```bash
oc get application shipper-onboarding-api -n openshift-gitops
```

2. Argo CD UI で Sync Status を確認
3. Git リポジトリの URL が正しいか確認

```bash
oc get application shipper-onboarding-api -n openshift-gitops -o yaml | grep repoURL
```

### buildah でイメージビルドが失敗する

**原因:** SCC (SecurityContextConstraints) が設定されていない

**解決方法:**

```bash
oc adm policy add-scc-to-user privileged -z pipeline
```

## 📚 参考資料

- [OpenShift Pipelines Documentation](https://docs.openshift.com/pipelines/latest/)
- [OpenShift GitOps Documentation](https://docs.openshift.com/gitops/latest/)
- [Argo CD Documentation](https://argo-cd.readthedocs.io/)
- [Kustomize Documentation](https://kustomize.io/)
- [Tekton Documentation](https://tekton.dev/docs/)

## 📄 ライセンス

このプロジェクトは MIT ライセンスの下で公開されています。
