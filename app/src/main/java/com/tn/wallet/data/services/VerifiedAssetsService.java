package com.tn.wallet.data.services;

import android.util.Log;

import com.google.gson.Gson;
import com.tn.wallet.api.NodeManager;
import com.tn.wallet.payload.DataEntry;
import com.tn.wallet.ui.auth.EnvironmentManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;

public class VerifiedAssetsService {

    private Map<String,String> verifiedAssetsMap = new HashMap<>();


    public VerifiedAssetsService() {
        Thread th = new Thread(() -> {
            String address = NodeManager.get().getAddress();
            URL url;
            HttpURLConnection con;
            BufferedReader in;
            String line;
            StringBuffer activeAssets = new StringBuffer();
            DataEntry[] dataEntriesTickers;
            DataEntry[] dataEntriesStatus;
            try {
                url = new URL(EnvironmentManager.get().current().getNodeUrl() + "/addresses/data/"+EnvironmentManager.get().current().getOracle()+"?matches=ticker_%3C.*");
                con = (HttpURLConnection) url.openConnection();
                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                while ((line = in.readLine()) != null) {
                    activeAssets.append(line);
                }
                Gson gson = new Gson();

                dataEntriesTickers = gson.fromJson(activeAssets.toString(), DataEntry[].class);

                for (DataEntry entry : dataEntriesTickers) {
                    activeAssets = new StringBuffer();
                    Log.i("Test", entry.value);
                    String asset_id = entry.key.replace("ticker_<", "").replace(">", "");
                    url = new URL(EnvironmentManager.get().current().getNodeUrl() + "/addresses/data/"+EnvironmentManager.get().current().getOracle()+"?matches=status_<" + asset_id + ">");
                    con = (HttpURLConnection) url.openConnection();
                    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    while ((line = in.readLine()) != null) {
                        activeAssets.append(line);
                    }
                    gson = new Gson();

                    dataEntriesStatus = gson.fromJson(activeAssets.toString(), DataEntry[].class);
                    Log.i("Test", dataEntriesStatus[0].value);
                    if (dataEntriesStatus[0].value.equals("2")){
                        verifiedAssetsMap.put(asset_id,entry.value);
                    }
                }
                in.close();
                con.disconnect();

            } catch (Exception ex) {
                ex.printStackTrace();
                Log.e("Wallet", "Error getting active assets from oracle: ", ex);
            }


        });
        th.start();
        //TODO: solve this without the join
        try {
            th.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Observable<Map<String, String>> getAllVerifiedAssets() {
        return Observable.just(verifiedAssetsMap);
//        return Observable.create(emitter -> {
//            verifiedAssetsReference.addListenerForSingleValueEvent(
//                    new ValueEventListener() {
//                        @Override
//                        public void onDataChange(DataSnapshot snap) {
//                            if (snap != null && snap.getValue() != null) {
//                                emitter.onNext((Map<String, String>) snap.getValue());
//                                emitter.onComplete();
//                            } else {
//                                emitter.onError(new IncorrectPinException());
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(DatabaseError error) {
//                            emitter.onError(error.toException());
//                        }
//                    });
//        });
    }

}