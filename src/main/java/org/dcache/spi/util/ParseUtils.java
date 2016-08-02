package org.dcache.spi.util;

import com.google.common.base.Joiner;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpEntity;
import org.indigo.cdmi.BackendCapability;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dcache.spi.dCacheStorageBackend;

import static org.indigo.cdmi.BackendCapability.CapabilityType;

public class ParseUtils
{
    public static JSONObject responseAsJson(HttpEntity response) throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        response.writeTo(os);
        String responseAsJson =  new String(os.toByteArray(), Charsets.UTF_8);
        return new JSONObject(responseAsJson);
    }

    public static List<String> jsonListToStringList(List<JSONObject> list)
    {
        List<String> capabilities = new ArrayList<String>(list.size());
        for (JSONObject obj: list) {
            capabilities.add(obj.toString());
        }
        return capabilities;
    }

    public static BackendCapability backendCapabilityFromJson(JSONObject obj, CapabilityType type)
    {
        Map<String, String> metadata = new HashMap<>();
        Map<String, String> capabilities = new HashMap<>();
        JSONObject backendCap = (JSONObject) obj.get("backendCapability");

        String name = backendCap.getString("name");
        JSONObject meta = backendCap.getJSONObject("metadata");
        List<String> transition = JsonUtils.jsonArrayToStringList(backendCap.getJSONArray("transition"));

        String cdmiRedundancy = meta.getString("cdmi_data_redundancy_provided");

        //List<String> cdmiGeoP = Arrays.asList(meta.getString("cdmi_geographic_placement_provided"));
        List<String> cdmiGeoP = JsonUtils.jsonArrayToStringList(
                                            meta.getJSONArray("cdmi_geographic_placement_provided"));

        String cdmiLatency = meta.getString("cdmi_latency_provided");

        metadata.put("cdmi_data_redundancy", cdmiRedundancy);
        metadata.put("cdmi_geographic_placement", listToGeoString(cdmiGeoP));
        metadata.put("cdmi_latency", cdmiLatency);

        capabilities.putAll(dCacheStorageBackend.capabilities);
        capabilities.put("cdmi_capabilities_allowed", capabiliesAllowed(transition, type));

        BackendCapability capability = new BackendCapability(name, type);
        capability.setMetadata(metadata);
        capability.setCapabilities(capabilities);
        return capability;
    }

    public static Map<String, String> metadataFromJson (JSONObject obj) {
        Map<String, String> metadata = new HashMap<>();
        JSONObject backendCap = (JSONObject) obj.get("backendCapability");

        JSONObject meta = backendCap.getJSONObject("metadata");

        String cdmiRedundancyP = meta.getString("cdmi_data_redundancy_provided");
        List<String> cdmiGeoPP = JsonUtils.jsonArrayToStringList(
                                        meta.getJSONArray("cdmi_geographic_placement_provided"));
        String cdmiLatencyP = meta.getString("cdmi_latency_provided");

        metadata.put("cdmi_data_redundancy_provided", cdmiRedundancyP);
        metadata.put("cdmi_geographic_placement_provided", listToGeoString(cdmiGeoPP));
        metadata.put("cdmi_latency_provided", cdmiLatencyP);
        return metadata;
    }

    private static String listToGeoString (List<String> cdmiGeoPlacement)
    {
        String result = cdmiGeoPlacement.stream().map((g) -> "\"" + g + "\"").collect(Collectors.joining(", "));
        System.out.println("cdmiGeo = " + result);
        return "[ " + result + "]";
    }

    private static String capabiliesAllowed (List<String> allowed, CapabilityType type)
    {
        List<String> listCapAllowed = new ArrayList<>(allowed.size());
        for (String s: allowed) {
            listCapAllowed.add("/cdmi_capabilities/" + backendCapTypeToString(type) + "/" + s + "/");
        }
        return Joiner.on(' ').skipNulls().join(listCapAllowed);
    }

    public static String backendCapTypeToString(CapabilityType type) {
        switch (type) {
            case CONTAINER:
                return "container";
            case DATAOBJECT:
                return "dataobject";
            default:
                return null;
        }
    }

    public static String fileTypeToCapType (String type) {
        switch (type) {
            case "DIR":
                return "container";
            case "REGULAR":
                return "dataobject";
            default:
                return null;
        }
    }
}
