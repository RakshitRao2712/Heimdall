package com.scraper.leadscraper.service;
import java.util.*;
/** Query variants preserve the user's category/location text; no invented nearby cities. */
public final class SearchQueries {
 private SearchQueries(){}
 public static List<String> variants(String query) {
  var result=new LinkedHashSet<String>();result.add(query.trim());
  for(String modifier:List.of("local", "independent", "small", "affordable", "best rated", "family owned", "nearby", "neighborhood", "established", "specialist")) result.add(modifier+" "+query.trim());
  for(String area:List.of("north", "south", "east", "west", "central", "northeast", "northwest", "southeast", "southwest")) {
   result.add(query.trim()+" in the "+area+" area");
   for(String modifier:List.of("local", "independent", "small", "affordable", "neighborhood"))result.add(modifier+" "+query.trim()+" in the "+area+" area");
  }
  return List.copyOf(result);
 }
 /** Direct browser discovery never falls back to an unqualified business query. */
 public static List<String> browserCopyrightVariants(String query,int minYear,int maxYear) {
  String base=query.trim();
  // Respect an explicitly supplied copyright expression instead of adding contradictory years.
  if(base.matches("(?is).*(?:copyright|©)[^0-9]{0,8}(?:19|20)\\d{2}.*"))return List.of(base);
  var location = java.util.regex.Pattern.compile("(?i)^(.+?)\\s+in\\s+(.+)$").matcher(base);
  if (location.matches()) {
   String place = location.group(2).strip();
   if (!(place.startsWith("\"") && place.endsWith("\""))) {
    place = "\"" + place.replace("\"", "") + "\"";
   }
   base = location.group(1).strip() + " " + place;
  }
  var result = new ArrayList<String>();
  for (int year = maxYear; year >= minYear; year--) {
   result.add(base + " \"copyright " + year + "\"");
  }
  return List.copyOf(result);
 }
 /** Search for the requested copyright years before spending time visiting broad business results. */
 public static List<String> copyrightVariants(String query,int minYear,int maxYear) {
  var result=new LinkedHashSet<String>();String base=query.trim();
  if(base.matches("(?is).*(?:copyright|©)[^0-9]{0,8}(?:19|20)\\d{2}.*"))return variants(base);
  // Bound the targeted phase so sparse exact-phrase matches cannot consume the entire budget.
  var targeted=new ArrayList<String>();
  for(int year=maxYear;year>=minYear;year--)targeted.add(base+" \"copyright "+year+"\"");
  for(int year=maxYear;year>=minYear;year--)targeted.add(base+" \"© "+year+"\"");
  for(int year=maxYear;year>=minYear;year--)targeted.add(base+" \"copyright\" \""+year+"\"");
  result.addAll(targeted.subList(0,Math.min(6,targeted.size())));
  result.addAll(variants(query));
  for(String variant:variants(query)) {
   if(variant.equals(base))continue;
   for(int year=maxYear;year>=minYear;year--)result.add(variant+" \"copyright\" \""+year+"\"");
  }
  return List.copyOf(result);
 }

}
