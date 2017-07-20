setwd("/home/fide/Workspace/wordsensepredictor/")
source("multiplot.R")
library(tidyverse);
library(plyr);
library(dplyr);
library(reshape2);
library(stringr);
library(ggplot2);
setwd("/home/fide/Workspace/wordsensepredictor/data/compare")
reject_threshhold = 0;
read_dataset <- function(file) {
  dataset <- read_delim(file, delim= '\t', escape_double = FALSE)  %>% 
    select(context_id, target, gold_sense_ids, predict_sense_ids, predict_score, correct);
  
  dataset$dataset <- file;
  dataset$predicted <- ifelse(dataset$predict_score > reject_threshhold, 1, 0);
  
  dataset$correct <- ifelse(dataset$correct == "true", 1, 0); # FIXME correct in eval code base
  dataset$correct[dataset$predicted == 0] = 0;
  
  return(dataset);
};

read_legacy_dataset <- function(file) {
  dataset <- read_delim(file, delim= '\t', escape_double = FALSE)  %>% 
    select(context_id, target, gold_sense_ids, predict_sense_ids, correct);
  
  dataset$predict_score = 0
  
  dataset$dataset <- file;
  
  dataset$predicted <- ifelse(dataset$predict_sense_ids != "-1", 1, 0);
  
  dataset$correct <- ifelse(dataset$correct == "true", 1, 0); # FIXME correct in eval code base
  dataset$correct[dataset$predicted == 0] = 0;
  
  return(dataset);
};

read_datasets <- function(...) {
  files <- list(...);
  datasets <- lapply(files, read_dataset);
  
  return(ldply(datasets));
}
data <- read_datasets(
  "cos_traditional_self",
  "cos_traditional_coocwords"
)

#data <- ldply(list(
#  read_legacy_dataset("clusterwords-filtered-reject.csv"),
#  read_legacy_dataset("simwords-filtered-reject.csv"),
# read_legacy_dataset("clusterwords-filtered-reject-all.csv"),
#  read_legacy_dataset("simwords-filtered-reject-all.csv")
#))

predicted = data %>% filter(predicted == 1);

mean_score <- ddply(predicted, .(correct, dataset), summarize, predict_score=mean(predict_score));

measures = data %>% group_by(dataset) %>% dplyr::summarize(
    n = n(),
    n_predicted = sum(predicted),
    n_correct = sum(correct),
    coverage = n_predicted / n,
    precision = n_correct / n_predicted,
    recall =  n_correct / n,
    f1_measure = 2*precision*recall / (precision + recall)
    );


p1 <- ggplot(data=predicted, aes(x=target, y=predict_score, color=dataset)) +
  geom_point() +
  geom_hline(aes(yintercept = predict_score, color=dataset), mean_score) +
  facet_grid(~correct) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1));


stats <- measures %>% melt(
  id.vars=c("dataset"),
  measure.vars=c("coverage", "precision", "recall", "f1_measure")
  );

p2 <- ggplot(data=stats, aes(x=variable, y=value, fill=dataset)) +
  geom_bar(stat="identity", position="dodge")
  theme(axis.text.x = element_text(angle = 90, hjust = 1));

multiplot(p1, p2)