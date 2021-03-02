## SampleDD

Sample patients who die during their stay from MIMIC-III, and their earliest associated text record.

### Usage

1. Download **NOTEEVENTS.csv** and **ADMISSIONS.csv** into a directory.
2. Run ``groovy sampledd.groovy <path to mimic directory> <train size> <valid size> <test size>``

This will produce sampled_patients.tsv with the sampled patients. Note: the train set is balanced 50/50, but the other sets are not.

3. Run ``groovy extract_earliest_texts.groovy <path to mimic directory>`` to extract the earliest text record associated with each patient visit. They will be in texts/.
