package de.autopolish.nativeapp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

final class Order {
    String id = "";
    String orderNumber = "";
    String firstName = "";
    String lastName = "";
    String email = "";
    String phone = "";
    String street = "";
    String postalCode = "";
    String city = "";
    String licensePlate = "";
    String mileage = "";
    String vehicle = "";
    String vin = "";
    String status = "offen";
    String createdAt = "";
    int grossCents = 0;
    JSONArray services = new JSONArray();

    static Order fromJson(JSONObject json) {
        Order value = new Order();
        value.id = json.optString("id");
        value.orderNumber = json.optString("orderNumber");
        value.firstName = json.optString("firstName");
        value.lastName = json.optString("lastName");
        value.email = json.optString("email");
        value.phone = json.optString("phone");
        value.street = json.optString("street");
        value.postalCode = json.optString("postalCode");
        value.city = json.optString("city");
        value.licensePlate = json.optString("licensePlate");
        value.mileage = json.optString("mileage");
        value.vehicle = json.optString("vehicle");
        value.vin = json.optString("vin");
        value.status = json.optString("status", "offen");
        value.createdAt = json.optString("createdAt");
        value.grossCents = json.optInt("grossCents");
        value.services = json.optJSONArray("services");
        if (value.services == null) value.services = new JSONArray();
        return value;
    }

    String customerName() {
        return (firstName + " " + lastName).trim();
    }

    String gross() {
        return NumberFormat.getCurrencyInstance(Locale.GERMANY).format(grossCents / 100.0);
    }
}
