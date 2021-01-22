package xyz.kandrac.appsignaturereader

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter


class MainActivity : AppCompatActivity() {

    private val myAdapter by lazy { MyAdapter }

    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO: request QUERY_ALL_PACKAGES permission for Android 11 +

        val allData = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            .map {
                val applicationName = packageManager.getApplicationLabel(it.applicationInfo)
                val applicationIcon = packageManager.getApplicationIcon(it.applicationInfo)
                val packageName = it.packageName
                val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    packageManager.getPackageInfo(it.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo.apkContentsSigners
                else
                    packageManager.getPackageInfo(it.packageName, PackageManager.GET_SIGNATURES).signatures

                PackageInfo(applicationIcon, applicationName.toString(), packageName, signatures.toList())
            }.sortedBy { it.name }

        findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = myAdapter
        }

        findViewById<EditText>(R.id.filter).addTextChangedListener { input ->
            input?.toString()?.let { text ->
                myAdapter.data = allData
                    .filter { it.packageName.contains(text) || it.name.contains(text) }
                    .sortedBy {
                    when {
                        it.name.startsWith(text) -> 0
                        it.packageName.startsWith(text) -> 1
                        else -> 2
                    }
                }
            }
        }

        findViewById<View>(R.id.sort_app_name).setOnClickListener {
            myAdapter.data = myAdapter.data.sortedBy { it.name }
        }

        findViewById<View>(R.id.sort_package_name).setOnClickListener {
            myAdapter.data = myAdapter.data.sortedBy { it.packageName }
        }

        myAdapter.data = allData
    }

    data class PackageInfo(val icon: Drawable, val name: String, val packageName: String, val signatures: Collection<Signature>)


    object MyAdapter : Adapter<MyAdapter.MyViewHolder>() {

        class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

            val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
            val appName: TextView = itemView.findViewById(R.id.app_name)
            val appPackageName: TextView = itemView.findViewById(R.id.package_name)
            val appSignatureHash: TextView = itemView.findViewById(R.id.app_signature_hash)

        }

        var data : List<PackageInfo> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val context = holder.itemView.context
            with(data[position]) {
                val commonSignatureApps = data
                    .filter { it != this }
                    .filter { that -> this.signatures.any { it in that.signatures } }

                holder.appIcon.setImageDrawable(icon)
                holder.appName.text = name
                holder.appPackageName.text = packageName
                holder.appSignatureHash.text = "Signature hash: " + signatures.map { it.hashCode().toString(16) }.joinToString("\n")
                holder.itemView.setOnClickListener {
                    AlertDialog.Builder(context)
                        .setTitle("Package specs")
                        .setMessage(
                            if (commonSignatureApps.isEmpty())
                                "This app doesn't share signature with any other application"
                            else
                                "This app has common signatures with these applications : ${commonSignatureApps.joinToString(", ") { it.name }}")
                        .show()
                }
            }
        }

        override fun getItemCount() = data.size
    }
}