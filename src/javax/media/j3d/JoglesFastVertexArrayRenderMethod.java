package javax.media.j3d;

/**
 * My override so I can add minimal code path for Android
 * @author phil
 *
 */
public class JoglesFastVertexArrayRenderMethod extends VertexArrayRenderMethod
{
	public static boolean ASSUME_A_LOT = true;

	@Override
	public boolean render(RenderMolecule rm, Canvas3D cv, RenderAtomListInfo ra, int dirtyBits)
	{

		if (!ASSUME_A_LOT)
		{
			return super.render(rm, cv, ra, dirtyBits);
		}
		
		// a no op now
		//GeometryArrayRetained geo = (GeometryArrayRetained) ra.geometry();
		//		geo.setVertexFormat((rm.useAlpha && ((geo.vertexFormat & GeometryArray.COLOR) != 0)), rm.textureBin.attributeBin.ignoreVertexColors,
		//				cv.ctx);

		if (rm.doInfinite)
		{
			cv.updateState(dirtyBits);
			while (ra != null)
			{
				renderGeo(ra, rm, cv);
				ra = ra.next;
			}
			return true;
		}

		boolean isVisible = false; // True if any of the RAs is visible.
		while (ra != null)
		{
			if (cv.ra == ra.renderAtom)
			{
				if (cv.raIsVisible)
				{
					cv.updateState(dirtyBits);
					renderGeo(ra, rm, cv);
					isVisible = true;
				}
			}
			else
			{
				if (!VirtualUniverse.mc.viewFrustumCulling || ra.renderAtom.localeVwcBounds.intersect(cv.viewFrustum))
				{
					cv.updateState(dirtyBits);
					cv.raIsVisible = true;
					renderGeo(ra, rm, cv);
					isVisible = true;
				}
				else
				{
					cv.raIsVisible = false;
				}
				cv.ra = ra.renderAtom;
			}

			ra = ra.next;
		}
		return isVisible;
	}

	void renderGeo(RenderAtomListInfo ra, RenderMolecule rm, Canvas3D cv)
	{
		if (!ASSUME_A_LOT)
		{
			super.renderGeo(ra, rm, cv);
			return;
		}
		GeometryArrayRetained geo = (GeometryArrayRetained) ra.geometry();
		boolean useAlpha = rm.useAlpha; 

		geo.execute(cv, ra.renderAtom, rm.isNonUniformScale, (useAlpha && ((geo.vertexFormat & GeometryArray.COLOR) != 0)), rm.alpha,
				cv.screen.screen, rm.textureBin.attributeBin.ignoreVertexColors);
	}
}