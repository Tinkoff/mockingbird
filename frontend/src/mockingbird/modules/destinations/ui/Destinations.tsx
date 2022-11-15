import React from 'react';
import type { Props as SelectProps } from 'src/mockingbird/components/form/Select';
import Select from 'src/mockingbird/components/form/Select';
import { useDestinations } from '../hooks';

type Props = SelectProps & {
  serviceId: string;
};

export default function Destinations({ serviceId, ...props }: Props) {
  const destinations = useDestinations(serviceId);
  return <Select {...props} options={destinations} />;
}
