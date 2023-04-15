import React from 'react';
import type { Props as SelectProps } from 'src/mockingbird/components/form/Select';
import Select from 'src/mockingbird/components/form/Select';
import { useSources } from '../hooks';

type Props = Omit<SelectProps, 'options'> & {
  serviceId: string;
};

export default function Sources({ serviceId, ...props }: Props) {
  const sources = useSources(serviceId);
  return <Select {...props} options={sources} />;
}
