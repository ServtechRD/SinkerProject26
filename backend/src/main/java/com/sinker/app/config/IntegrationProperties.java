package com.sinker.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 外部整合：PDCA 重算、ERP 採購單等，URL 與帳密由設定檔／環境變數注入。
 */
@ConfigurationProperties(prefix = "app.integrations")
public class IntegrationProperties {

    private Auth auth = new Auth();
    private Pdca pdca = new Pdca();
    private Erp erp = new Erp();
    private ErpProduct erpProduct = new ErpProduct();
    private ErpVendor erpVendor = new ErpVendor();

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

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

    public ErpProduct getErpProduct() {
        return erpProduct;
    }

    public void setErpProduct(ErpProduct erpProduct) {
        this.erpProduct = erpProduct;
    }

    public ErpVendor getErpVendor() {
        return erpVendor;
    }

    public void setErpVendor(ErpVendor erpVendor) {
        this.erpVendor = erpVendor;
    }

    /** 三個外部系統共用的 JWT 取得設定 */
    public static class Auth {
        private String tokenUrl = "";
        private String account = "";
        private String salasana = "";

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public String getSalasana() {
            return salasana;
        }

        public void setSalasana(String salasana) {
            this.salasana = salasana;
        }
    }

    public static class Pdca {
        /** 是否呼叫外部 PDCA recompute API */
        private boolean enabled = false;
        /** 完整 URL（含 path），例如 https://pdca.example.com/api/recompute */
        private String recomputeUrl = "";

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
    }

    public static class Erp {
        /** 是否呼叫外部 ERP 採購單建立 API */
        private boolean enabled = false;
        private String purchaseOrderUrl = "";

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
    }

    public static class ErpProduct {
        private boolean enabled = false;
        private String productListUrl = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProductListUrl() {
            return productListUrl;
        }

        public void setProductListUrl(String productListUrl) {
            this.productListUrl = productListUrl;
        }
    }

    public static class ErpVendor {
        private boolean enabled = false;
        private String vendorListUrl = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getVendorListUrl() {
            return vendorListUrl;
        }

        public void setVendorListUrl(String vendorListUrl) {
            this.vendorListUrl = vendorListUrl;
        }
    }
}
