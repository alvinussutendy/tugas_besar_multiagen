<?php
	include ('../../OpenConnection.php'); 
?>
<!DOCTYPE html>
<html>
	<head>
		<title>eLibrary</title>
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<!-- OPTIONAL -->
		<link rel="stylesheet" href="../../style/style.css">
		<link rel="stylesheet" href="../../lib/font-awesome.min.css">
		<link rel="stylesheet" href="../../lib/font-awesome.css">
	</head>
	<body>
	<div class="isi">
			<?php
				session_start();
				include ('../../header.php');
			?>
			<div class="middle">
				<?php
					include ('../../side.php');
					$username=$_SESSION['username'];

					$queryShowPeminjaman = "SELECT DISTINCT 
												*,
												IF(DATEDIFF(peminjaman.tgl_kembali,peminjaman.tgl_pinjam)-7<=0,0,
													DATEDIFF(peminjaman.tgl_kembali,peminjaman.tgl_pinjam)-7) AS 'overdue', 
												IF(DATEDIFF(peminjaman.tgl_kembali,peminjaman.tgl_pinjam)-7<=0,0,
													(DATEDIFF(peminjaman.tgl_kembali,peminjaman.tgl_pinjam)-7)*5000) AS fine 
											FROM 
												buku inner join peminjaman 
												on buku.id_buku = peminjaman.id_buku inner join anggota 
												on peminjaman.username = anggota.username
											WHERE peminjaman.username='$username'";
					$query = $conn->getQuery();
				?>
				<div class="article">
					<div class="opening2"><p id="judul">Borrowing History</p></div>
					<div class="main">
						<table>
							<tr><th>Book Code</th><th>Book Title</th><th>Author</th><th>Borrow Date</th>
							<th>Return Date</th><th>Overdue</th><th>Fine</th></tr>
							<?php
								if($result = $query->query($queryShowPeminjaman)){
									while($row = $result->fetch_array()){
										echo "<tr>";
										echo "<td>".$row['id_buku']."</td>";
										echo "<td>".$row['judul']."</td>";
										echo "<td>".$row['pengarang']."</td>";
										echo "<td>".$row['tgl_pinjam']."</td>";
										echo "<td>".$row['tgl_kembali']."</td>";
										echo "<td>".$row['overdue']."</td>";
										echo "<td>".$row['fine']."</td>";
										echo "</tr>";
									}
								}
							?>
						</table>
					</div>
					<div class="closing">
						<p>&copy; 2016 Maria Veronica - eLibrary fow Web Based Programming</p>
					</div>
				</div>
			</div>
		</div>
	</body>
</html>
