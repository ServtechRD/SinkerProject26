package com.sinker.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 外部整合：PDCA 重算、ERP 採購單等，URL 與帳密由設定檔／環境變數注入。
 */
@ConfigurationProperties(prefix = "app.integrations")
public class IntegrationProperties {

    private Pdca pdca = new Pdca();
    private Erp erp = new Erp();

    public Pdca getPdca() {
        return pdca;
    }

    public void setPdca(Pdca pdca) {
        this.pdca = pdca;
    }

    public Erp getErp() {
        return erp;
    }

    public void setErp(Erp erp) {
        this.erp = erp;
    }

    public static class Pdca {
        /** 是否呼叫外部 PDCA recompute API */
        private boolean enabled = false;
        /** 完整 URL（含 path），例如 https://pdca.example.com/api/recompute */
        private String recomputeUrl = "";
        private String username = "";
        private String password = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRecomputeUrl() {
            return recomputeUrl;
        }

        public void setRecomputeUrl(String recomputeUrl) {
            this.recomputeUrl = recomputeUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Erp {
        /** 是否呼叫外部 ERP 採購單建立 API */
        private boolean enabled = false;
        private String purchaseOrderUrl = "";
        private String username = "";
        private String password = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPurchaseOrderUrl() {
            return purchaseOrderUrl;
        }

        public void setPurchaseOrderUrl(String purchaseOrderUrl) {
            this.purchaseOrderUrl = purchaseOrderUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
