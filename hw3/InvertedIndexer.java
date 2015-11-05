import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;

//import org.apache.hadoop.mapreduce.InputSplit;
//import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;




public class InvertedIndexer {

    // The mapper class, you should modify T1, T2, T3, T4 to your desired
    // types
    public static class InvertedIndexMapper extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {


        private Text word = new Text();

        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            // TODO: implement your map function



			FileSplit fileSplit = (FileSplit)reporter.getInputSplit();
            String filename = fileSplit.getPath().getName();


            String line = value.toString().toLowerCase().replaceAll("[^a-zA-Z]+"," ").trim();
            StringTokenizer tokenizer = new StringTokenizer(line);

            while (tokenizer.hasMoreTokens()) {

                word.set(tokenizer.nextToken());

                output.collect(word, new Text(filename));

            }
        }
    }


    // The reducer class, you should modify T1, T2, T3, T4 to your desired
    // types
    public static class InvertedIndexReducer extends MapReduceBase
            implements Reducer<Text, Text, Text, Text> {

        //public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            // TODO: implement your reduce function


            // Count
            Map<String, Integer> files = new HashMap<String, Integer>();
            while(values.hasNext()) {
            	Text val = values.next();
                if (files.containsKey(val.toString())) {
                    files.put(val.toString(), files.get(val.toString()) + 1);
                } else {
                    files.put(val.toString(), 1);
                }
            }

            // Sort
            TreeSet<Vocab> tree = new TreeSet<Vocab>();

            for (String file : files.keySet()) {
                Vocab vocab = new Vocab(file, files.get(file));
                tree.add(vocab);
            }

            String outputString = key.toString();

            for (Vocab vocab : tree.descendingSet()) {
                outputString += "\n" + vocab.toString();
                //outputString += "\n<" + file + ", " + files.get(file) + ">";
            }

            System.out.println("key is: " + key + " and output is: " + outputString);

            output.collect(new Text(outputString), new Text("\n"));
        }
    }




    /**
      * The actual main() method for our program; this is the
      * "driver" for the MapReduce job.
      */
    public static void main(String[] args) throws Exception {
        // TODO: configure the hadoop job and run the job

        JobConf conf = new JobConf(InvertedIndexer.class);
		conf.setJobName("wordcount");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);
		//conf.setOutputValueClass(Text.class);

		conf.setMapperClass(InvertedIndexMapper.class);
		//conf.setCombinerClass(InvertedIndexReducer.class);
		conf.setReducerClass(InvertedIndexReducer.class);

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

        conf.set("mapred.textoutputformat.separator", "");


		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));

		JobClient.runJob(conf);
    }

    public static class Vocab implements Comparable<Vocab> {

        private String chapter;
        private int count;

        public Vocab(String chapter, int count) {
            this.chapter = chapter;
            this.count = count;
        }

        public String getChapter() {
            return this.chapter;
        }

        public int getCount() {
            return this.count;
        }

        public void setChapter(String chapter) {
            this.chapter = chapter;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String toString() {
            return (new StringBuilder())
                .append("<")
                .append(this.chapter)
                .append(", ")
                .append(this.count)
                .append(">")
                .toString();
        }

        public boolean equals(Object otherObj) {
            if (otherObj instanceof Vocab) {
                Vocab other = (Vocab) otherObj;

                return this.chapter.equals(other.chapter) && this.count == other.count;
            }
            else {
                return false;
            }
        }

        public int compareTo(Vocab o) {

            if (this.count == o.count) {
                return o.chapter.compareTo(this.chapter);
            }
            else {
                return Integer.compare(this.count, o.count);
            }
        }

        @Override
        public int hashCode() {
            return this.chapter.hashCode();
        }
    }

}

