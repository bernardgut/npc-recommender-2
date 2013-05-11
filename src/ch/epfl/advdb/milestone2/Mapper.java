/**
 * 
 */
package ch.epfl.advdb.milestone2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import ch.epfl.advdb.milestone2.io.ClusterCenter;
import ch.epfl.advdb.milestone2.io.Fetchers;
import ch.epfl.advdb.milestone2.io.Pair;

/**
 * @author Bernard Gütermann
 *
 */
public class Mapper {
	
	public static int run(String[] args, final int K) throws IOException{
		//LOAD CENTROIDS
		Configuration c = new Configuration();
		c.setInt("K", K);
		System.out.println("Fetch Clusters after IMDB iteration :"+Counters.ITERATIONS_IMDB);
		c.set("CPATH", args[2]+"/clusterIMDB"+Counters.ITERATIONS_IMDB);
		Pair<ClusterCenter,String>[] imdbClusters = Fetchers.fetchClusters(c);
		System.out.println("Fetch Clusters after Netflix iteration :"+Counters.ITERATIONS_NETFLIX);
		c.set("CPATH", args[2]+"/clusterNetflix"+Counters.ITERATIONS_NETFLIX);
		Pair<ClusterCenter,String>[] netflixClusters = Fetchers.fetchClusters(c);
		System.out.println("# IMDB;Netflix centers : "+imdbClusters.length+";"+netflixClusters.length);
		//JACCARD SIMILARITY ALGO
		ArrayList<Pair<Integer, Integer>> associations = new ArrayList<Pair<Integer, Integer>>();
		for (Pair<ClusterCenter, String> c1 : imdbClusters){
			double similarity  = 0;
			double maxSimilarity = 0;
			int maxID=-1;
			for (Pair<ClusterCenter, String> c2 : netflixClusters){
				similarity = jaccardSimilarity(c1.y, c2.y);
				if (similarity>maxSimilarity){
					maxSimilarity=similarity;
					maxID=c2.x.getClusterID();
				}
			}
			//Save mapping : <IMDB, Netflix>
			associations.add(new Pair<Integer, Integer>(c1.x.getClusterID(), maxID));
		}
		return writeToDisk(args, associations);
	}

	/**
	 * 
	 * @param associations
	 */
	private static int writeToDisk(
			String[] args, ArrayList<Pair<Integer, Integer>> associations) {
		Path op = new Path(args[2]+"/mapping/part0");
		Configuration c= new Configuration();
		FileSystem fs;
		try{
			fs  = FileSystem.get(c);
			BufferedWriter br=new BufferedWriter(new OutputStreamWriter(fs.create(op,true)));
			String line;
			for(Pair<Integer, Integer> p : associations){
				line= p.x+","+p.y+"\n";
				br.write(line);
			}
			br.close();
		}
		catch (IOException e){
			System.err.println("Mapper : unable to write pairs");
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	
	/**
	 * 
	 * @param y1
	 * @param y2
	 * @return
	 */
	private static double jaccardSimilarity(String y1, String y2) {
		String[] s1 = y1.split(",");
		String[] s2 = y2.split(",");
		double n = 0;
		for (String e1 : s1){
			for (String e2 :s2){
				if (e1.equals(e2))
					n++;
			}
		}
		//|AuB|=|A|+|B|-|AnB|
		return n/(s1.length+s2.length-n);
	}
}