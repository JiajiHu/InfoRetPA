package cs276.pa4;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Util {
  public static Map<Query,List<Document>> loadTrainData (String feature_file_name) throws Exception {
    Map<Query, List<Document>> result = new HashMap<Query, List<Document>>();

    File feature_file = new File(feature_file_name);
    if (!feature_file.exists() ) {
      System.err.println("Invalid feature file name: " + feature_file_name);
      return null;
    }

    BufferedReader reader = new BufferedReader(new FileReader(feature_file));
    String line = null, anchor_text = null;
    Query query = null;
    Document doc = null;
    int numQuery=0; int numDoc=0;
    while ((line = reader.readLine()) != null) {
      String[] tokens = line.split(":", 2);
      String key = tokens[0].trim();
      String value = tokens[1].trim();

      if (key.equals("query")){
        query = new Query(value);
        numQuery++;
        result.put(query, new ArrayList<Document>());
      } else if (key.equals("url")) {
        doc = new Document();
        doc.url = new String(value);
        result.get(query).add(doc);
        numDoc++;
      } else if (key.equals("title")) {
        doc.title = new String(value);
      } else if (key.equals("header"))
      {
        if (doc.headers == null)
          doc.headers =  new ArrayList<String>();
        doc.headers.add(value);
      } else if (key.equals("body_hits")) {
        if (doc.body_hits == null)
          doc.body_hits = new HashMap<String, List<Integer>>();
        String[] temp = value.split(" ", 2);
        String term = temp[0].trim();
        List<Integer> positions_int;

        if (!doc.body_hits.containsKey(term))
        {
          positions_int = new ArrayList<Integer>();
          doc.body_hits.put(term, positions_int);
        } else
          positions_int = doc.body_hits.get(term);

        String[] positions = temp[1].trim().split(" ");
        for (String position : positions)
          positions_int.add(Integer.parseInt(position));

      } else if (key.equals("body_length"))
        doc.body_length = Integer.parseInt(value);
      else if (key.equals("pagerank"))
        doc.page_rank = Integer.parseInt(value);
      else if (key.equals("anchor_text")) {
        anchor_text = value;
        if (doc.anchors == null)
          doc.anchors = new HashMap<String, Integer>();
      }
      else if (key.equals("stanford_anchor_count"))
        doc.anchors.put(anchor_text, Integer.parseInt(value));      
    }

    reader.close();
    System.err.println("# Signal file " + feature_file_name + ": number of queries=" + numQuery + ", number of documents=" + numDoc);

    return result;
  }

  public static Map<String,Double> loadDFs(String dfFile) throws IOException {
    Map<String,Double> dfs = new HashMap<String, Double>();
    double totalDocCount = 98998;
    BufferedReader br = new BufferedReader(new FileReader(dfFile));
    String line;
    while((line=br.readLine())!=null){
      line = line.trim();
      if(line.equals("")) continue;
      String[] tokens = line.split("\\s+");
      dfs.put(tokens[0], Double.parseDouble(tokens[1]));
    }
    br.close();
    
    for (String term : dfs.keySet()) {
      dfs.put(term,
          Math.log((totalDocCount + 1) / (dfs.get(term) + 1.0)));
    }
    dfs.put("unseen term", Math.log((totalDocCount + 1.0)));
    
    return dfs;
  }

  /* query -> (url -> score) */
  public static Map<String, Map<String, Double>> loadRelData(String rel_file_name) throws IOException{
    Map<String, Map<String, Double>> result = new HashMap<String, Map<String, Double>>();

    File rel_file = new File(rel_file_name);
    if (!rel_file.exists() ) {
      System.err.println("Invalid feature file name: " + rel_file_name);
      return null;
    }

    BufferedReader reader = new BufferedReader(new FileReader(rel_file));
    String line = null, query = null, url = null;
    int numQuery=0; 
    int numDoc=0;
    while ((line = reader.readLine()) != null) {
      String[] tokens = line.split(":", 2);
      String key = tokens[0].trim();
      String value = tokens[1].trim();

      if (key.equals("query")){
        query = value;
        result.put(query, new HashMap<String, Double>());
        numQuery++;
      } else if (key.equals("url")){
        String[] tmps = value.split(" ", 2);
        url = tmps[0].trim();
        double score = Double.parseDouble(tmps[1].trim());
        result.get(query).put(url, score);
        numDoc++;
      }
    }	
    reader.close();
    System.err.println("# Rel file " + rel_file_name + ": number of queries=" + numQuery + ", number of documents=" + numDoc);
    
    return result;
  }

  public static void main(String[] args) {
    try {
      System.out.print(loadRelData(args[0]));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static Map<String, Double> getQueryFreqs(Query q, boolean subLinear) {
    Map<String, Double> tfQuery = new HashMap<String, Double>();
    for (String word : q.words) {
      if (tfQuery.containsKey(word)) {
        tfQuery.put(word.toLowerCase(), tfQuery.get(word.toLowerCase()) + 1.0);
      } else {
        tfQuery.put(word.toLowerCase(), 1.0);
      }
    }
    if (subLinear) {
      for (String word : tfQuery.keySet()) {
        tfQuery.put(word.toLowerCase(), 1.0 + Math.log(tfQuery.get(word.toLowerCase())));
      }
    }
    return tfQuery;
  }

  public static Map<String, Double> parseURL(String url) {
    Map<String, Double> u_tf = new HashMap<String, Double>();
    if (url == null)
      return u_tf;
    String[] tokens = url.toLowerCase().trim().split("[^a-z0-9]");
    for (int i = 1; i < tokens.length; i++) {
      String token = tokens[i];
      if (u_tf.containsKey(token))
        u_tf.put(token, u_tf.get(token) + 1.0);
      else
        u_tf.put(token, 1.0);
    }
    return u_tf;
  }

  public static Map<String, Double> parseTitle(String title) {
    Map<String, Double> t_tf = new HashMap<String, Double>();
    if (title == null)
      return t_tf;
    String[] tokens = title.toLowerCase().trim().split("\\s+");
    for (String token : tokens) {
      if (t_tf.containsKey(token))
        t_tf.put(token, t_tf.get(token) + 1.0);
      else
        t_tf.put(token, 1.0);
    }
    return t_tf;
  }

  public static Map<String, Double> parseHeader(List<String> header) {
    Map<String, Double> h_tf = new HashMap<String, Double>();
    if (header == null)
      return h_tf;
    for (String head : header) {
      String[] tokens = head.toLowerCase().trim().split("\\s+");
      for (String token : tokens) {
        if (h_tf.containsKey(token))
          h_tf.put(token, h_tf.get(token) + 1.0);
        else
          h_tf.put(token, 1.0);
      }
    }
    return h_tf;
  }

  public static Map<String, Double> parseAnchor(Map<String, Integer> anchor) {
    Map<String, Double> a_tf = new HashMap<String, Double>();
    if (anchor == null)
      return a_tf;
    for (String anchor_text : anchor.keySet()) {
      for (String token : anchor_text.toLowerCase().split("\\s+")) {
        if (a_tf.containsKey(token))
          a_tf.put(token, (double) (anchor.get(anchor_text) + a_tf.get(token)));
        else
          a_tf.put(token, (double) anchor.get(anchor_text));
      }
    }
    return a_tf;
  }

  public static Map<String, Double> parseBody(Map<String, List<Integer>> body) {
    Map<String, Double> b_tf = new HashMap<String, Double>();
    if (body == null)
      return b_tf;
    for (String token : body.keySet()) {
      b_tf.put(token, (double) body.get(token).size());
    }
    return b_tf;
  }

  public static Map<Field, Map<String, Double>> getDocTermFreqs(Document d, Query q,
      boolean subLinear) {
    Map<Field, Map<String, Double>> tfs = new HashMap<Field, Map<String, Double>>();
    Map<Field, Map<String, Double>> q_tfs = new HashMap<Field, Map<String, Double>>();

    tfs.put(Field.URL, parseURL(d.url));
    tfs.put(Field.TITLE, parseTitle(d.title));
    tfs.put(Field.HEADER, parseHeader(d.headers));
    tfs.put(Field.ANCHOR, parseAnchor(d.anchors));
    tfs.put(Field.BODY, parseBody(d.body_hits));

    for (Field field : Field.values()) {
      q_tfs.put(field, new HashMap<String, Double>());
    }
    // loop through query terms increasing relevant tfs
    for (String queryWord : q.words) {
      for (Field field : Field.values()) {
        if (tfs.get(field).containsKey(queryWord)) {
          if (subLinear)
            q_tfs.get(field).put(queryWord,
                1.0 + Math.log(tfs.get(field).get(queryWord)));
          else
            q_tfs.get(field).put(queryWord, tfs.get(field).get(queryWord));
        } else
          q_tfs.get(field).put(queryWord, 0.0);
      }
    }
    return q_tfs;
  }
}
