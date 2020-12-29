package uk.co.firstchoice_cs.core.api.legacyAPI.models;

import java.util.ArrayList;
import java.util.List;

public class RootProducts {
    private Page Paging = new Page();
    private List<ProductDetails> Products = new ArrayList<ProductDetails>();

    public List<ProductDetails> getProducts() {

        return Products;
    }

    public Page getPaging() {
        return Paging;
    }

    public void setPaging(final Page Paging) {
        this.Paging = Paging;
    }

    public ProductDetails findProduct(String sku) {
        for (ProductDetails p : Products) {
            if (p.getPartName().equals(sku)) {
                return p;
            }
        }
        return null;
    }

    public ProductDetails findProductByBarCode(String barcode) {
        for (ProductDetails p : Products) {
            if (p.getBarcode().equals(barcode)) {
                return p;
            }
        }
        return null;
    }

    public RootProducts clone() {
        RootProducts r = new RootProducts();
        r.Paging = Paging.clone();
        r.Products.addAll(Products);
        return r;
    }
}
