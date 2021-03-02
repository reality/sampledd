@Grab('org.apache.commons:commons-csv:1.2')
import org.apache.commons.csv.CSVParser
import static org.apache.commons.csv.CSVFormat.*

import java.nio.file.Paths

def mimicDirectory = new File(args[0])
def spts = [:]
new File('sampled_patients.tsv').splitEachLine('\t') {
  spts[it[1]] = it
}

def earliestRecords = [:]
new File(mimicDirectory, 'NOTEEVENTS.csv').withReader { reader ->
  CSVParser csv = new CSVParser(reader, DEFAULT.withHeader())

  for(record in csv.iterator()) {
    if(spts.containsKey(record['HADM_ID']) && record['CHARTTIME'] != '') {
      def time = new Date().parse('yyyy-MM-dd hh:mm:ss', record['CHARTTIME'])
      if(earliestRecords.containsKey(record['HADM_ID'])) {
        if(time < earliestRecords[record['HADM_ID']]['t']) {
          println 'Updated ' + record['HADM_ID']
          earliestRecords[record['HADM_ID']] = [ t: time, r: record ]
        }
      } else {
        println 'Found ' + record['HADM_ID']
        earliestRecords[record['HADM_ID']] = [ t: time, r: record ]
      }
    }
  }
}

def outDir = new File('texts')
if(!outDir.exists()) { outDir.mkdir() }

earliestRecords.each { k, v ->
  new File(outDir, k + '.txt').text = v['r']['TEXT'].replaceAll('\n', '').replaceAll('\\s+', ' ').replaceAll('\\.', '. ')
}



