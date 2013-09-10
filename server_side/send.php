<?php

	//receive json data 
	$json = file_get_contents('php://input');
	$json_obj = json_decode($json);

	echo $json_obj;

	foreach ($json_obj as $obj) {
		echo $obj['device_id'];
		if($obj['device_id']!=''){
			$handle = fopen($obj['device_id']."txt", 'a');
			fwrite($handle, $obj['type'].','.$obj['name'].','.$obj['mac'].','.$obj['time']);
		}
	}

?>