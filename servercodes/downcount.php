<?php 

    error_reporting(E_ALL); 
    ini_set('display_errors',1); 

    include('dbcon.php');

    $id = (int)$_POST['id'];
    $stmt = $con->prepare("update furniture set download_count = download_count + 1 where id = $id");
    $stmt->execute();

?>
