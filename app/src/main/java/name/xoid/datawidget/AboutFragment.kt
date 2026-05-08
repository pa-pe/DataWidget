package name.xoid.datawidget

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import name.xoid.datawidget.databinding.FragmentAboutBinding

import androidx.core.content.pm.PackageInfoCompat

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val context = requireContext()
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)

        binding.textviewAbout.text = "Data Widget\nVersion: $versionName\nBuild: $versionCode\n\nDisplay remote JSON data on your home screen."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
