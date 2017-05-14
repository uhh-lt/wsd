<?php

define("PROJECT_ROOT", __DIR__);
define("API_LIMIT_MESSAGE", 'Your key is not valid or the daily requests limit has been reached.');

require_once PROJECT_ROOT . '/vendor/autoload.php';

$envfile = isset($argv[0]) ? ".${argv[0]}env" : ".env";
$dotenv = new Dotenv\Dotenv(PROJECT_ROOT, $envfile);
$dotenv->load();

$api_limit_reached = false;

function curl_call($url) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($ch, CURLOPT_ENCODING, 'gzip');
    $response = curl_exec($ch);
    curl_close($ch);
    return $response;
}

function build_cache_string($endpoint, $params) {
    $param_str = implode('_',$params);
    $cache_key = "${endpoint}_${param_str}.json";
    return $cache_key;

}

function cached_babelnet_api_call($endpoint, $params) {
    $cache_key = build_cache_string($endpoint, $params);
    $cache_dir = getenv('BABELNET_CACHE_FOLDER');
    $cache_file = "$cache_dir/$cache_key";
    $parms_str = var_export($params, true);
    echo "Retrieving: $endpoint $parms_str ... ";
    if (file_exists($cache_file)) {
        $json = json_decode(file_get_contents($cache_file), true);
        echo "response already in cache: $cache_file \n";
    } else {
        $json = babelnet_api_call($endpoint, $params);

        if($GLOBALS['api_limit_reached']) {
            file_put_contents($cache_file . "__ERROR___", json_encode($json));
            echo "stopped, because of API limit reached.";
        } else {
            file_put_contents($cache_file, json_encode($json));
            echo "download finished.\n";
        }

    }
    return ($json);
}

function babelnet_api_call($endpoint, $params) {

    $service_url = "http://babelnet.io/v3/$endpoint";

    $params['key'] = getenv('BABELNET_KEY');

    $url = $service_url . '?' . http_build_query($params);

    $json = json_decode(curl_call($url), true);

    if (array_key_exists("message", $json)) {
        $message =  $json["message"];
        if (strpos($message, API_LIMIT_MESSAGE) !== false) {
            $GLOBALS['api_limit_reached'] = true;
        }
        echo "ERROR: ". $json["message"] . "\n";
    }

    return $json;
}


function babelnet_api_glosses_call($id) {
    $response = cached_babelnet_api_call('getSynset', array( 'id' => $id));
    return array_map(function($g) {return $g['gloss'];}, $response['glosses']);
}

function babelnet_api_hypernym_word_call($hypernymId) {
    $synset = cached_babelnet_api_call('getSynset', array( 'id' => $hypernymId));
    $mainSense = isset($synset['mainSense']) ? $synset['mainSense'] : null;
    if (strpos($mainSense, '#') !== false) {
        $mainSense = substr($mainSense, 0, strpos($mainSense, '#'));
    }
    return $mainSense;
}

function babelnet_api_hypernym_ids_call($id) {
    $response = cached_babelnet_api_call('getEdges', array( 'id' => $id));
    $hypernyms = array_filter($response, function($v, $ignore) {
        return $v['pointer']['relationGroup'] == 'HYPERNYM'; // Do not filter by language, $v['language'] == 'EN'
        // The babelnet.org frontend seems to ignore the language identifier for the "pointer"
        // See https://github.com/fmarten/wsd/issues/18
    }, ARRAY_FILTER_USE_BOTH);

    return array_unique(array_map(function($h) {return $h['target'];}, $hypernyms));
}

function babelnet_api_hyperhypernyms_ids_call($id) {
    $hypernymIds = babelnet_api_hypernym_ids_call($id);
    $nestedIds = array_map("babelnet_api_hypernym_ids_call", $hypernymIds);
    if ($nestedIds) {
        return array_unique(array_merge(...$nestedIds));
    } else {
        return array();
    }
}

function babelnet_api_hypernyms_call($id) {
    $hypernymIds = babelnet_api_hypernym_ids_call($id);
    return array_values(array_filter(array_map("babelnet_api_hypernym_word_call", $hypernymIds)));
}

function babelnet_api_hyperhypernyms_call($id) {
    $hyperhypernymIds = babelnet_api_hyperhypernyms_ids_call($id);
    return array_values(array_filter(array_map("babelnet_api_hypernym_word_call", $hyperhypernymIds)));
}

function enrich_babelnet_sense_with_hypernyms_and_glosses($sense) {
    $id = $sense['synsetID']['id'];
    return array(
        'lemma' => $sense['lemma'],
        'glosses' => babelnet_api_glosses_call($id),
        'hypernyms' => babelnet_api_hypernyms_call($id),
        'hyperhypernyms' => babelnet_api_hyperhypernyms_call($id),
        'synsetId' => $id
    );
}

function array_unique_on($array, $key) {
    array_unique_with_callback($array, function($e) use ($key) {return $e[$key]; });
}

/*
 * Makes $array unique in the sense, that the return value of $callback
 * evaluated on each element is unique
 */
function array_unique_with_callback($array, callable $callback) {
    if (!$array) return array();
    
    $unique_keys_array = array();
    foreach ($array as $obj) {
        $unique_keys_array[$callback($obj)] = $obj;
    }
    return array_values($unique_keys_array);
}

function write_babelnet_samples_to_file($word, $path) {
    $senses = cached_babelnet_api_call('getSenses', array( 'word' => $word, 'lang' => 'EN'));
    $get_synset_id = function($el) {return $el['synsetID']['id'];};
    $unique_senses = array_unique_with_callback($senses, $get_synset_id);
    $enriched_sense = array_map('enrich_babelnet_sense_with_hypernyms_and_glosses', $unique_senses);
    $senses_with_actual_hpyernyms = array_filter($enriched_sense, function($v, $k) {
        return !empty($v['hypernyms']);
    }, ARRAY_FILTER_USE_BOTH);
    file_put_contents($path, json_encode(array_values($senses_with_actual_hpyernyms)));
    //write_as_spark_format_with_mulitjson_file($senses_with_actual_hpyernyms, $path);
}

function write_as_spark_format_with_mulitjson_file($senses, $path) {
    // Multiline JSON format as described in the documentation.
    // See: http://spark.apache.org/docs/latest/sql-programming-guide.html#json-datasets

    $test_cases_per_sense = array_map("convert_babelnet_sense_to_test_cases", $senses);
    $test_cases = $test_cases_per_sense ? array_merge(...$test_cases_per_sense) : array();
    $content = implode("\n", array_map('json_encode', $test_cases));
    file_put_contents($path, $content);
}


function convert_babelnet_sense_to_test_cases($sense) {
    $test_cases = array();
    foreach ($sense["glosses"] as $gloss) {
        $test_case = array(
            'lemma' => $sense['lemma'],
            'gloss' => $gloss,
            'hypernyms' => $sense['hypernyms'],
            'synsetId' => $sense['synsetId']
        );
        array_push($test_cases, $test_case);
    }
    return $test_cases;
}

$query_words = array_filter(explode("\n", file_get_contents(getenv('WORDLIST_FILE'))));

    #array(
#    'python', 'ruby', 'java', 'jaguar', 'bank', 'client', 'leaf',
#    'board', 'book', 'control', 'date', 'color', 'family', 'force',
#    'image',
#    'life', 'number', 'paper', 'part', 'people', 'power', 'sight', 'sound',
#    'state', 'trace', 'way', 'window'
#);

foreach ($query_words as $word) {
    echo "Downloading for '$word'.\n--------------------\n";
    $output_folder = getenv('OUTPUT_FOLDER');
    $output_file = "${word}.json";
    $output_path = "$output_folder/$output_file";
    write_babelnet_samples_to_file($word, $output_path);
    if($GLOBALS['api_limit_reached']) {
        echo "Partial results written to file '${output_file}' in folder '${output_folder}'.\n";
        echo "Existing because of API limit reached.";
        exit(1);
    } else {
        echo "Successfully '${output_file}' written to '${output_folder}'.\n";
    }

}