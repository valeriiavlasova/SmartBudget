package pl.edu.finanseosobiste.data

import androidx.room.*

@Entity(tableName="accounts")
data class Account(@PrimaryKey(autoGenerate=true) val id: Long=0, val name:String, val openingMinor:Long=0)
@Entity(tableName="cash_entries", foreignKeys=[ForeignKey(entity=Account::class,parentColumns=["id"],childColumns=["accountId"],onDelete=ForeignKey.RESTRICT)],indices=[Index("accountId")])
data class CashEntry(@PrimaryKey(autoGenerate=true) val id:Long=0, val accountId:Long, val title:String, val amountMinor:Long, val timestamp:Long, val kind:String, val transferKey:String="")
data class AccountBalance(val id:Long,val name:String,val balanceMinor:Long)
