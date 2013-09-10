<?php

	//receive json data 
	$json = file_get_contents('php://input');
	$obj = json_decode($json);
	
	echo $json;

	if($obj['device_id']!=''){
		$handle = fopen($obj['device_id'], 'a');
		fwrite($handle, $obj['type'].','.$obj['name'].','.$obj['mac'].','.$obj['time']);
	}
	//} else {
	//	fopen($obj['device_id'], 'w');
	//}

?>